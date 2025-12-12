package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.AccountTreeVO;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import com.finance.financesystem.service.IFinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务报表控制器 - 终极修复版
 * 
 * 核心修复：资产负债表自动计算本年利润
 * 
 * 会计恒等式：资产 = 负债 + 所有者权益
 * 所有者权益 = 实收资本 + 留存收益 + 本年利润
 * 本年利润 = 收入 - 费用（自动计算）
 */
@RestController
@RequestMapping("/report")
@CrossOrigin(origins = "*")
public class FinanceReportController {

    @Autowired
    private IFinanceSplitService splitService;
    
    @Autowired
    private IFinanceAccountService accountService;
    
    @Autowired
    private IFinanceTransactionService transactionService;

    /**
     * 1. 资产负债表
     * 
     * 关键：自动计算本年利润 = 收入 - 费用
     */
    @GetMapping("/balance-sheet")
    public Map<String, Object> getBalanceSheet() {
        List<FinanceAccount> allAccounts = accountService.list();
        Map<Long, BigDecimal> balanceMap = calculateAllBalances();
        
        // ★★★ 核心修复：先计算本年利润 ★★★
        BigDecimal netProfit = calculateNetProfit(allAccounts, balanceMap);

        // 构建资产树
        AccountTreeVO assetTree = buildTreeByType("ASSET", allAccounts, balanceMap);
        
        // 构建负债树
        AccountTreeVO liabilityTree = buildTreeByType("LIABILITY", allAccounts, balanceMap);
        
        // 构建权益树，并注入本年利润
        AccountTreeVO equityTree = buildEquityTreeWithProfit(allAccounts, balanceMap, netProfit);

        Map<String, Object> result = new HashMap<>();
        result.put("asset", assetTree);
        result.put("liability", liabilityTree);
        result.put("equity", equityTree);
        
        BigDecimal totalAsset = assetTree != null ? assetTree.getAmount() : BigDecimal.ZERO;
        BigDecimal totalLiab = liabilityTree != null ? liabilityTree.getAmount() : BigDecimal.ZERO;
        BigDecimal totalEquity = equityTree != null ? equityTree.getAmount() : BigDecimal.ZERO;
        
        result.put("totalAsset", totalAsset);
        result.put("totalLiabilityEquity", totalLiab.add(totalEquity));
        result.put("netProfit", netProfit);  // 返回本年利润供前端显示
        
        return result;
    }
    
    /**
     * ★★★ 核心方法：计算本年利润 ★★★
     * 
     * 本年利润 = 收入类科目余额 - 费用类科目余额
     */
    private BigDecimal calculateNetProfit(List<FinanceAccount> allAccounts, Map<Long, BigDecimal> balanceMap) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (FinanceAccount acc : allAccounts) {
            String type = acc.getAccountType() != null ? acc.getAccountType() : "";
            String code = acc.getAccountCode() != null ? acc.getAccountCode() : "";
            String name = acc.getAccountName() != null ? acc.getAccountName() : "";
            BigDecimal balance = balanceMap.getOrDefault(acc.getAccountId(), BigDecimal.ZERO);
            
            // 收入类科目
            if ("INCOME".equals(type) || code.startsWith("4") || code.equals("6001") || name.contains("收入")) {
                totalIncome = totalIncome.add(balance);
            }
            // 费用类科目
            else if ("EXPENSE".equals(type) || code.startsWith("5") || code.startsWith("64") || code.startsWith("66")) {
                totalExpense = totalExpense.add(balance);
            }
        }
        
        // 本年利润 = 收入 - 费用
        return totalIncome.subtract(totalExpense);
    }
    
    /**
     * 构建权益树，并将本年利润注入
     */
    private AccountTreeVO buildEquityTreeWithProfit(List<FinanceAccount> allAccounts, 
            Map<Long, BigDecimal> balanceMap, BigDecimal netProfit) {
        
        // 先构建基础权益树
        AccountTreeVO equityTree = buildTreeByType("EQUITY", allAccounts, balanceMap);
        
        if (equityTree == null) {
            // 如果没有权益科目，创建一个虚拟的
            equityTree = new AccountTreeVO();
            equityTree.setId(0L);
            equityTree.setName("所有者权益");
            equityTree.setCode("3000");
            equityTree.setType("EQUITY");
            equityTree.setAmount(BigDecimal.ZERO);
        }
        
        // 查找「本年利润」科目并更新其金额
        boolean foundProfitAccount = updateProfitInTree(equityTree, netProfit);
        
        // 如果没找到本年利润科目，添加一个
        if (!foundProfitAccount && netProfit.compareTo(BigDecimal.ZERO) != 0) {
            AccountTreeVO profitNode = new AccountTreeVO();
            profitNode.setId(-1L);  // 虚拟ID
            profitNode.setName("本年利润（自动计算）");
            profitNode.setCode("3103");
            profitNode.setType("EQUITY");
            profitNode.setAmount(netProfit);
            equityTree.getChildren().add(profitNode);
        }
        
        // 重新计算权益总额
        recalculateTreeAmount(equityTree);
        
        return equityTree;
    }
    
    /**
     * 在树中查找本年利润科目并更新金额
     */
    private boolean updateProfitInTree(AccountTreeVO node, BigDecimal netProfit) {
        if (node == null) return false;
        
        String name = node.getName() != null ? node.getName() : "";
        String code = node.getCode() != null ? node.getCode() : "";
        
        // 找到本年利润科目
        if (name.contains("本年利润") || code.equals("3103") || code.equals("3131")) {
            node.setAmount(netProfit);
            node.setName("本年利润");  // 统一名称
            return true;
        }
        
        // 递归查找子节点
        for (AccountTreeVO child : node.getChildren()) {
            if (updateProfitInTree(child, netProfit)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 重新计算树节点金额（从叶子向上汇总）
     */
    private BigDecimal recalculateTreeAmount(AccountTreeVO node) {
        if (node == null) return BigDecimal.ZERO;
        
        if (node.getChildren().isEmpty()) {
            // 叶子节点，直接返回金额
            return node.getAmount() != null ? node.getAmount() : BigDecimal.ZERO;
        }
        
        // 有子节点，金额 = 子节点之和
        BigDecimal sum = BigDecimal.ZERO;
        for (AccountTreeVO child : node.getChildren()) {
            sum = sum.add(recalculateTreeAmount(child));
        }
        node.setAmount(sum);
        return sum;
    }

    /**
     * 2. 利润表
     */
    @GetMapping("/income")
    public Map<String, Object> getIncomeStatement() {
        Map<String, Object> result = new HashMap<>();
        
        List<FinanceAccount> allAccounts = accountService.list();
        Set<Long> postedTxIds = getPostedTransactionIds();
        List<FinanceSplit> allSplits = splitService.list();
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalFinanceExp = BigDecimal.ZERO;
        
        Map<Long, FinanceAccount> accountMap = allAccounts.stream()
            .collect(Collectors.toMap(FinanceAccount::getAccountId, a -> a));
        
        for (FinanceSplit split : allSplits) {
            if (!postedTxIds.contains(split.getTransactionId())) {
                continue;
            }
            
            FinanceAccount acc = accountMap.get(split.getAccountId());
            if (acc == null) continue;
            
            String type = acc.getAccountType() != null ? acc.getAccountType() : "";
            String code = acc.getAccountCode() != null ? acc.getAccountCode() : "";
            String name = acc.getAccountName() != null ? acc.getAccountName() : "";
            BigDecimal amt = split.getAmount() != null ? split.getAmount() : BigDecimal.ZERO;
            int dir = split.getDcDirection() != null ? split.getDcDirection() : 0;
            
            // 收入类：贷方增加收入
            if ("INCOME".equals(type) || code.startsWith("4") || code.equals("6001") || name.contains("收入")) {
                if (dir == -1) {
                    totalIncome = totalIncome.add(amt);
                } else {
                    totalIncome = totalIncome.subtract(amt);
                }
            }
            // 费用/成本类：借方增加费用
            else if ("EXPENSE".equals(type) || code.startsWith("5") || code.startsWith("64") || code.startsWith("66")) {
                if (dir == 1) {
                    if (code.equals("6401") || code.startsWith("64") || name.contains("成本")) {
                        totalCost = totalCost.add(amt);
                    } else if (code.startsWith("6603") || name.contains("财务费用")) {
                        totalFinanceExp = totalFinanceExp.add(amt);
                    } else {
                        totalExpense = totalExpense.add(amt);
                    }
                }
            }
        }
        
        BigDecimal grossProfit = totalIncome.subtract(totalCost);
        BigDecimal operatingProfit = grossProfit.subtract(totalExpense).subtract(totalFinanceExp);
        BigDecimal netProfit = operatingProfit;
        
        result.put("revenue", totalIncome);
        result.put("cost", totalCost);
        result.put("grossProfit", grossProfit);
        result.put("expense", totalExpense);
        result.put("financeExpense", totalFinanceExp);
        result.put("operatingProfit", operatingProfit);
        result.put("netProfit", netProfit);
        
        return result;
    }

    /**
     * 3. 现金流量表
     */
    @GetMapping("/cashflow")
    public Map<String, Object> getCashflowStatement() {
        Map<String, Object> result = new HashMap<>();
        
        List<FinanceAccount> allAccounts = accountService.list();
        Set<Long> postedTxIds = getPostedTransactionIds();
        List<FinanceSplit> allSplits = splitService.list();
        List<FinanceTransaction> allTxList = transactionService.list();
        
        // 现金科目ID集合
        Set<Long> cashAccountIds = allAccounts.stream()
            .filter(a -> {
                String code = a.getAccountCode() != null ? a.getAccountCode() : "";
                String name = a.getAccountName() != null ? a.getAccountName() : "";
                return code.equals("1001") || code.equals("1002") ||
                       name.contains("货币资金") || name.contains("现金") || name.contains("银行存款");
            })
            .map(FinanceAccount::getAccountId)
            .collect(Collectors.toSet());
        
        Map<Long, String> txDescMap = allTxList.stream()
            .collect(Collectors.toMap(
                FinanceTransaction::getTransactionId, 
                t -> t.getDescription() != null ? t.getDescription() : "",
                (a, b) -> a
            ));
        
        BigDecimal salesCashIn = BigDecimal.ZERO;
        BigDecimal purchaseCashOut = BigDecimal.ZERO;
        BigDecimal salaryCashOut = BigDecimal.ZERO;
        BigDecimal otherCashIn = BigDecimal.ZERO;
        BigDecimal otherCashOut = BigDecimal.ZERO;
        
        for (FinanceSplit split : allSplits) {
            if (!postedTxIds.contains(split.getTransactionId())) continue;
            if (!cashAccountIds.contains(split.getAccountId())) continue;
            
            String txDesc = txDescMap.getOrDefault(split.getTransactionId(), "");
            BigDecimal amt = split.getAmount() != null ? split.getAmount() : BigDecimal.ZERO;
            boolean isDebit = split.getDcDirection() != null && split.getDcDirection() == 1;
            
            if (isDebit) {
                if (txDesc.contains("销售") || txDesc.contains("收款")) {
                    salesCashIn = salesCashIn.add(amt);
                } else {
                    otherCashIn = otherCashIn.add(amt);
                }
            } else {
                if (txDesc.contains("采购") || txDesc.contains("购买") || txDesc.contains("入库")) {
                    purchaseCashOut = purchaseCashOut.add(amt);
                } else if (txDesc.contains("工资") || txDesc.contains("薪酬")) {
                    salaryCashOut = salaryCashOut.add(amt);
                } else {
                    otherCashOut = otherCashOut.add(amt);
                }
            }
        }
        
        BigDecimal operatingCashNet = salesCashIn.subtract(purchaseCashOut)
            .subtract(salaryCashOut).add(otherCashIn).subtract(otherCashOut);
        
        result.put("salesCashIn", salesCashIn);
        result.put("purchaseCashOut", purchaseCashOut);
        result.put("salaryCashOut", salaryCashOut);
        result.put("otherCashIn", otherCashIn);
        result.put("otherCashOut", otherCashOut);
        result.put("operatingCashNet", operatingCashNet);
        result.put("investingCashNet", BigDecimal.ZERO);
        result.put("financingCashNet", BigDecimal.ZERO);
        result.put("totalCashChange", operatingCashNet);
        
        return result;
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 计算所有科目余额
     */
    private Map<Long, BigDecimal> calculateAllBalances() {
        List<FinanceSplit> splits = splitService.list();
        List<FinanceAccount> accounts = accountService.list();
        Set<Long> postedTxIds = getPostedTransactionIds();
        
        Map<Long, String> typeMap = accounts.stream()
            .collect(Collectors.toMap(FinanceAccount::getAccountId, 
                a -> a.getAccountType() != null ? a.getAccountType() : "ASSET"));
        
        Map<Long, BigDecimal> debitMap = new HashMap<>();
        Map<Long, BigDecimal> creditMap = new HashMap<>();
        
        for (FinanceSplit split : splits) {
            if (!postedTxIds.contains(split.getTransactionId())) {
                continue;
            }
            
            Long accId = split.getAccountId();
            BigDecimal amt = split.getAmount() != null ? split.getAmount() : BigDecimal.ZERO;
            
            if (split.getDcDirection() != null && split.getDcDirection() == 1) {
                debitMap.merge(accId, amt, BigDecimal::add);
            } else {
                creditMap.merge(accId, amt, BigDecimal::add);
            }
        }
        
        Map<Long, BigDecimal> result = new HashMap<>();
        
        for (FinanceAccount acc : accounts) {
            Long accId = acc.getAccountId();
            BigDecimal debit = debitMap.getOrDefault(accId, BigDecimal.ZERO);
            BigDecimal credit = creditMap.getOrDefault(accId, BigDecimal.ZERO);
            String type = typeMap.get(accId);
            
            BigDecimal balance;
            if (isDebitBalanceAccount(type)) {
                balance = debit.subtract(credit);
            } else {
                balance = credit.subtract(debit);
            }
            
            result.put(accId, balance);
        }
        
        return result;
    }
    
    private boolean isDebitBalanceAccount(String accountType) {
        return "ASSET".equals(accountType) || "EXPENSE".equals(accountType);
    }
    
    private Set<Long> getPostedTransactionIds() {
        List<FinanceTransaction> allTx = transactionService.list();
        return allTx.stream()
            .filter(tx -> tx.getStatus() == null || "POSTED".equals(tx.getStatus()))
            .map(FinanceTransaction::getTransactionId)
            .collect(Collectors.toSet());
    }
    
    /**
     * 按科目类型构建树
     */
    private AccountTreeVO buildTreeByType(String accountType, List<FinanceAccount> allAccounts, 
            Map<Long, BigDecimal> balanceMap) {
        
        List<FinanceAccount> roots = allAccounts.stream()
            .filter(a -> accountType.equals(a.getAccountType()))
            .filter(a -> a.getParentId() == null || a.getParentId() == 0)
            .collect(Collectors.toList());
        
        if (roots.isEmpty()) {
            AccountTreeVO virtualRoot = new AccountTreeVO();
            virtualRoot.setId(0L);
            virtualRoot.setName(getTypeName(accountType));
            virtualRoot.setCode("");
            virtualRoot.setType(accountType);
            virtualRoot.setAmount(BigDecimal.ZERO);
            
            List<FinanceAccount> topAccounts = allAccounts.stream()
                .filter(a -> accountType.equals(a.getAccountType()))
                .collect(Collectors.toList());
            
            for (FinanceAccount acc : topAccounts) {
                AccountTreeVO child = buildTree(acc.getAccountId(), allAccounts, balanceMap);
                if (child != null) {
                    virtualRoot.getChildren().add(child);
                    virtualRoot.setAmount(virtualRoot.getAmount().add(child.getAmount()));
                }
            }
            
            return virtualRoot;
        }
        
        if (roots.size() > 1) {
            AccountTreeVO virtualRoot = new AccountTreeVO();
            virtualRoot.setId(0L);
            virtualRoot.setName(getTypeName(accountType));
            virtualRoot.setCode("");
            virtualRoot.setType(accountType);
            virtualRoot.setAmount(BigDecimal.ZERO);
            
            for (FinanceAccount root : roots) {
                AccountTreeVO child = buildTree(root.getAccountId(), allAccounts, balanceMap);
                if (child != null) {
                    virtualRoot.getChildren().add(child);
                    virtualRoot.setAmount(virtualRoot.getAmount().add(child.getAmount()));
                }
            }
            return virtualRoot;
        }
        
        return buildTree(roots.get(0).getAccountId(), allAccounts, balanceMap);
    }
    
    private String getTypeName(String type) {
        switch (type) {
            case "ASSET": return "资产";
            case "LIABILITY": return "负债";
            case "EQUITY": return "所有者权益";
            case "INCOME": return "收入";
            case "EXPENSE": return "费用";
            default: return type;
        }
    }
    
    private AccountTreeVO buildTree(Long rootId, List<FinanceAccount> allAccounts, 
            Map<Long, BigDecimal> balanceMap) {
        
        FinanceAccount rootAccount = allAccounts.stream()
            .filter(a -> a.getAccountId().equals(rootId))
            .findFirst().orElse(null);
        
        if (rootAccount == null) return null;

        AccountTreeVO node = new AccountTreeVO();
        node.setId(rootAccount.getAccountId());
        node.setName(rootAccount.getAccountName());
        node.setCode(rootAccount.getAccountCode());
        node.setType(rootAccount.getAccountType());
        node.setDirection(rootAccount.getBalanceDirection());

        BigDecimal selfBalance = balanceMap.getOrDefault(rootId, BigDecimal.ZERO);
        
        List<FinanceAccount> children = allAccounts.stream()
            .filter(a -> rootId.equals(a.getParentId()))
            .collect(Collectors.toList());

        if (children.isEmpty()) {
            node.setAmount(selfBalance);
        } else {
            BigDecimal childSum = BigDecimal.ZERO;
            for (FinanceAccount child : children) {
                AccountTreeVO childNode = buildTree(child.getAccountId(), allAccounts, balanceMap);
                if (childNode != null) {
                    node.getChildren().add(childNode);
                    childSum = childSum.add(childNode.getAmount());
                }
            }
            node.setAmount(selfBalance.add(childSum));
        }
        
        return node;
    }
}