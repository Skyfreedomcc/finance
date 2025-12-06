package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@CrossOrigin(origins = "*") // 允许前端访问
public class FinanceReportController {

    @Autowired
    private IFinanceSplitService splitService;
    @Autowired
    private IFinanceAccountService accountService;

    @GetMapping("/summary")
    public Map<String, Object> getReportSummary() {
        // 1. 初始化统计桶 (全部归零)
        BigDecimal totalAsset = BigDecimal.ZERO;      // 资产
        BigDecimal totalLiability = BigDecimal.ZERO;  // 负债
        BigDecimal totalEquity = BigDecimal.ZERO;     // 权益
        BigDecimal totalIncome = BigDecimal.ZERO;     // 收入
        BigDecimal totalExpense = BigDecimal.ZERO;    // 费用
        BigDecimal cashInflow = BigDecimal.ZERO;      // 现金流入
        BigDecimal cashOutflow = BigDecimal.ZERO;     // 现金流出

        // 2. 获取所有科目，建立 "ID -> 科目类型" 的映射表
        // 这样我们才知道每一笔分录是属于资产还是费用
        List<FinanceAccount> accounts = accountService.list();
        Map<Long, String> typeMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        for (FinanceAccount acc : accounts) {
            typeMap.put(acc.getAccountId(), acc.getAccountType());
            nameMap.put(acc.getAccountId(), acc.getAccountName());
        }

        // 3. 获取所有分录 (这就是您在凭证查询里看到的那 6000 多块钱)
        List<FinanceSplit> splits = splitService.list();

        // 4. 开始算账！(核心算法)
        for (FinanceSplit split : splits) {
            Long accId = split.getAccountId();
            String type = typeMap.get(accId); // 查这笔钱是什么类型
            String name = nameMap.getOrDefault(accId, "");
            BigDecimal amount = split.getAmount();
            int direction = split.getDcDirection(); // 1=借(Debit), -1=贷(Credit)

            if (type == null || amount == null) continue;

            // --- 资产 (Asset) ---
            // 规则：借方(+)增加，贷方(-)减少
            if ("ASSET".equals(type)) {
                if(direction == 1) {
                    totalAsset = totalAsset.add(amount);
                    // 现金流计算：如果是现金或银行增加，记为流入
                    if (name.contains("现金") || name.contains("银行")) {
                        cashInflow = cashInflow.add(amount);
                    }
                } else {
                    totalAsset = totalAsset.subtract(amount);
                    // 现金流计算：如果是现金或银行减少，记为流出
                    if (name.contains("现金") || name.contains("银行")) {
                        cashOutflow = cashOutflow.add(amount);
                    }
                }
            } 
            // --- 负债 (Liability) ---
            // 规则：贷方(+)增加，借方(-)减少
            else if ("LIABILITY".equals(type)) {
                if(direction == -1) totalLiability = totalLiability.add(amount);
                else totalLiability = totalLiability.subtract(amount);
            } 
            // --- 权益 (Equity) ---
            // 规则：贷方(+)增加
            else if ("EQUITY".equals(type)) {
                if(direction == -1) totalEquity = totalEquity.add(amount);
                else totalEquity = totalEquity.subtract(amount);
            } 
            // --- 收入 (Income) ---
            // 规则：贷方(+)增加
            else if ("INCOME".equals(type)) {
                if(direction == -1) totalIncome = totalIncome.add(amount);
                else totalIncome = totalIncome.subtract(amount);
            } 
            // --- 费用 (Expense) ---
            // 规则：借方(+)增加
            else if ("EXPENSE".equals(type)) {
                if(direction == 1) totalExpense = totalExpense.add(amount);
                else totalExpense = totalExpense.subtract(amount);
            }
        }

        // 5. 计算利润 = 收入 - 费用
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        // 6. 打包发给前端
        Map<String, Object> result = new HashMap<>();
        result.put("assets", totalAsset);
        result.put("liabilities", totalLiability);
        // 资产负债表里的权益 = 初始权益 + 本期利润
        result.put("equity", totalEquity.add(netProfit)); 
        
        result.put("income", totalIncome);
        result.put("expense", totalExpense);
        result.put("profit", netProfit);
        
        // 净现金流
        result.put("cashNet", cashInflow.subtract(cashOutflow));

        return result;
    }
}