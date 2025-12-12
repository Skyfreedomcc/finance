package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import com.finance.financesystem.service.IFinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 凭证/交易控制器
 * 
 * 支持：
 * 1. 凭证录入（草稿/直接过账）
 * 2. 过账审核
 * 3. 科目明细账查询
 * 4. 科目余额汇总
 */
@RestController
@RequestMapping("/financeTransaction")
@CrossOrigin(origins = "*")
public class FinanceTransactionController {

    @Autowired
    private IFinanceTransactionService transactionService;

    @Autowired
    private IFinanceSplitService splitService;

    @Autowired
    private IFinanceAccountService accountService;

    // ==================== 凭证列表 ====================
    
    @GetMapping("/list")
    public List<Map<String, Object>> getList() {
        List<FinanceTransaction> transactions = transactionService.list(
            new QueryWrapper<FinanceTransaction>().orderByDesc("voucher_date", "transaction_id")
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (FinanceTransaction tx : transactions) {
            Map<String, Object> map = new HashMap<>();
            map.put("transactionId", tx.getTransactionId());
            map.put("voucherDate", tx.getVoucherDate());
            map.put("description", tx.getDescription());
            map.put("status", tx.getStatus());
            
            // 计算凭证总金额（借方合计）
            QueryWrapper<FinanceSplit> qw = new QueryWrapper<>();
            qw.eq("transaction_id", tx.getTransactionId());
            qw.eq("dc_direction", 1);
            List<FinanceSplit> debitSplits = splitService.list(qw);
            BigDecimal totalAmount = debitSplits.stream()
                .map(FinanceSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            map.put("totalAmount", totalAmount);
            
            result.add(map);
        }
        return result;
    }

    // ==================== 添加凭证 ====================
    
    @PostMapping("/add")
    public Map<String, Object> addTransaction(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析参数
            String voucherDateStr = (String) body.get("voucherDate");
            String description = (String) body.get("description");
            String status = (String) body.getOrDefault("status", "POSTED");  // 默认直接过账
            List<Map<String, Object>> splitsData = (List<Map<String, Object>>) body.get("splits");
            
            if (splitsData == null || splitsData.isEmpty()) {
                result.put("code", 400);
                result.put("message", "分录不能为空");
                return result;
            }
            
            // 创建凭证
            FinanceTransaction tx = new FinanceTransaction();
            tx.setVoucherDate(LocalDate.parse(voucherDateStr));
            tx.setDescription(description);
            tx.setStatus(status);  // ✅ 使用前端传递的状态
            transactionService.save(tx);
            
            Long txId = tx.getTransactionId();
            
            // 创建分录
            for (Map<String, Object> splitData : splitsData) {
                FinanceSplit split = new FinanceSplit();
                split.setTransactionId(txId);
                
                Object accIdObj = splitData.get("accountId");
                if (accIdObj instanceof Integer) {
                    split.setAccountId(((Integer) accIdObj).longValue());
                } else if (accIdObj instanceof Long) {
                    split.setAccountId((Long) accIdObj);
                }
                
                split.setSummary((String) splitData.get("summary"));
                
                Object dirObj = splitData.get("dcDirection");
                if (dirObj instanceof Integer) {
                    split.setDcDirection((Integer) dirObj);
                }
                
                Object amtObj = splitData.get("amount");
                if (amtObj instanceof Number) {
                    split.setAmount(new BigDecimal(amtObj.toString()));
                }
                
                splitService.save(split);
            }
            
            result.put("code", 200);
            result.put("message", status.equals("DRAFT") ? "草稿保存成功" : "凭证保存并过账成功");
            result.put("transactionId", txId);
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "保存失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    // ==================== 批量过账 ====================
    
    @PostMapping("/post")
    public Map<String, Object> postTransactions(@RequestBody List<Long> transactionIds) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (transactionIds == null || transactionIds.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "请选择要过账的凭证");
                return result;
            }
            
            // 批量更新状态为 POSTED
            UpdateWrapper<FinanceTransaction> uw = new UpdateWrapper<>();
            uw.in("transaction_id", transactionIds);
            uw.set("status", "POSTED");
            transactionService.update(uw);
            
            result.put("code", 200);
            result.put("msg", "过账成功，共处理 " + transactionIds.size() + " 张凭证");
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "过账失败: " + e.getMessage());
        }
        
        return result;
    }

    // ==================== 科目明细账 ====================
    
    @GetMapping("/ledger/{accountId}")
    public Map<String, Object> getLedger(@PathVariable Long accountId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取科目信息
        FinanceAccount account = accountService.getById(accountId);
        result.put("account", account);
        
        // 获取该科目的所有分录
        QueryWrapper<FinanceSplit> qw = new QueryWrapper<>();
        qw.eq("account_id", accountId);
        List<FinanceSplit> splits = splitService.list(qw);
        
        // 获取所有相关凭证
        List<Long> txIds = splits.stream()
            .map(FinanceSplit::getTransactionId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, FinanceTransaction> txMap = new HashMap<>();
        if (!txIds.isEmpty()) {
            List<FinanceTransaction> txList = transactionService.listByIds(txIds);
            txList.forEach(tx -> txMap.put(tx.getTransactionId(), tx));
        }
        
        // 构建明细列表
        List<Map<String, Object>> entries = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        // 判断科目余额方向
        boolean isDebitBalance = account != null && 
            ("ASSET".equals(account.getAccountType()) || "EXPENSE".equals(account.getAccountType()));
        
        for (FinanceSplit split : splits) {
            FinanceTransaction tx = txMap.get(split.getTransactionId());
            
            // 只显示已过账的（状态为 POSTED 或 状态为空的老数据）
            if (tx == null) {
                continue;
            }
            String txStatus = tx.getStatus();
            if (txStatus != null && !"POSTED".equals(txStatus)) {
                continue;
            }
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("splitId", split.getSplitId());
            entry.put("transactionId", split.getTransactionId());
            entry.put("voucherId", split.getTransactionId());
            entry.put("date", tx.getVoucherDate());
            entry.put("summary", split.getSummary());
            entry.put("dcDirection", split.getDcDirection());
            entry.put("amount", split.getAmount());
            
            // 计算累计余额
            if (split.getDcDirection() != null && split.getDcDirection() == 1) {
                totalDebit = totalDebit.add(split.getAmount());
                if (isDebitBalance) {
                    runningBalance = runningBalance.add(split.getAmount());
                } else {
                    runningBalance = runningBalance.subtract(split.getAmount());
                }
            } else {
                totalCredit = totalCredit.add(split.getAmount());
                if (isDebitBalance) {
                    runningBalance = runningBalance.subtract(split.getAmount());
                } else {
                    runningBalance = runningBalance.add(split.getAmount());
                }
            }
            
            entry.put("balance", runningBalance);
            entry.put("balanceDirection", runningBalance.compareTo(BigDecimal.ZERO) >= 0 ? "借" : "贷");
            
            entries.add(entry);
        }
        
        result.put("entries", entries);
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("finalBalance", runningBalance);
        
        return result;
    }

    // ==================== 科目余额汇总 ====================
    
    @GetMapping("/ledger/summary")
    public List<Map<String, Object>> getLedgerSummary() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有科目
        List<FinanceAccount> accounts = accountService.list();
        
        // 获取所有已过账凭证的ID（状态为 POSTED 或 null）
        List<FinanceTransaction> allTxList = transactionService.list();
        Set<Long> postedTxIds = allTxList.stream()
            .filter(tx -> tx.getStatus() == null || "POSTED".equals(tx.getStatus()))
            .map(FinanceTransaction::getTransactionId)
            .collect(Collectors.toSet());
        
        // 获取所有分录
        List<FinanceSplit> allSplits = splitService.list();
        
        // 按科目汇总
        Map<Long, BigDecimal> debitMap = new HashMap<>();
        Map<Long, BigDecimal> creditMap = new HashMap<>();
        
        for (FinanceSplit split : allSplits) {
            // 只统计已过账的
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
        
        // 构建返回数据
        for (FinanceAccount acc : accounts) {
            Long accId = acc.getAccountId();
            BigDecimal debit = debitMap.getOrDefault(accId, BigDecimal.ZERO);
            BigDecimal credit = creditMap.getOrDefault(accId, BigDecimal.ZERO);
            
            // 跳过没有交易的科目
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            
            // 计算余额
            BigDecimal balance;
            boolean isDebitBalance = "ASSET".equals(acc.getAccountType()) || "EXPENSE".equals(acc.getAccountType());
            if (isDebitBalance) {
                balance = debit.subtract(credit);
            } else {
                balance = credit.subtract(debit);
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("accountId", accId);
            item.put("accountCode", acc.getAccountCode());
            item.put("accountName", acc.getAccountName());
            item.put("accountType", acc.getAccountType());
            item.put("totalDebit", debit);
            item.put("totalCredit", credit);
            item.put("balance", balance);
            item.put("balanceDirection", isDebitBalance ? "借" : "贷");
            
            result.add(item);
        }
        
        return result;
    }
}