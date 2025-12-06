package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
public class FinanceReportController {

    @Autowired
    private IFinanceSplitService splitService;
    @Autowired
    private IFinanceAccountService accountService;

    /**
     * 获取三大报表所需的所有数据
     * 为了简化，我们一次性计算并返回
     */
    @GetMapping("/summary")
    public Map<String, Object> getReportSummary() {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取所有科目，建立 ID -> Type 的映射
        List<FinanceAccount> accounts = accountService.list();
        Map<Long, String> accountTypeMap = new HashMap<>();
        Map<Long, String> accountNameMap = new HashMap<>();
        for (FinanceAccount acc : accounts) {
            accountTypeMap.put(acc.getAccountId(), acc.getAccountType());
            accountNameMap.put(acc.getAccountId(), acc.getAccountName());
        }

        // 2. 获取所有分录
        List<FinanceSplit> splits = splitService.list();

        // 3. 按照“张三老板”的逻辑进行统计
        BigDecimal totalAsset = BigDecimal.ZERO; // 家当 (资产)
        BigDecimal totalLiability = BigDecimal.ZERO; // 欠债 (负债)
        BigDecimal totalEquity = BigDecimal.ZERO; // 净家底 (权益)
        BigDecimal totalIncome = BigDecimal.ZERO; // 卖饭钱 (收入)
        BigDecimal totalExpense = BigDecimal.ZERO; // 买菜钱 (费用)
        BigDecimal cashInflow = BigDecimal.ZERO; // 现金流入
        BigDecimal cashOutflow = BigDecimal.ZERO; // 现金流出

        for (FinanceSplit split : splits) {
            String type = accountTypeMap.get(split.getAccountId());
            String name = accountNameMap.getOrDefault(split.getAccountId(), "");
            BigDecimal amt = split.getAmount();
            // 注意：数据库里 amount 可能是绝对值，结合 dc_direction 判断正负
            // 这里假设 amount 已经处理好符号，或者我们简单累加：
            // 资产/费用：借方为正；负债/权益/收入：贷方为正。
            // 为了简化，我们假设数据库 amount 存储的是绝对值，配合 dcDirection (1=借, -1=贷)

            int dir = split.getDcDirection();

            if ("ASSET".equals(type)) {
                // 资产：借增贷减
                totalAsset = totalAsset.add(amt.multiply(new BigDecimal(dir)));

                // 现金流量表逻辑：如果是"现金"或"银行"科目
                if (name.contains("现金") || name.contains("银行")) {
                    if (dir == 1) cashInflow = cashInflow.add(amt); // 借方=钱进来了
                    else cashOutflow = cashOutflow.add(amt);        // 贷方=钱出去了
                }
            } else if ("LIABILITY".equals(type)) {
                // 负债：贷增借减
                totalLiability = totalLiability.add(amt.multiply(new BigDecimal(-dir)));
            } else if ("EQUITY".equals(type)) {
                // 权益：贷增借减
                totalEquity = totalEquity.add(amt.multiply(new BigDecimal(-dir)));
            } else if ("INCOME".equals(type)) {
                // 收入：贷增借减
                totalIncome = totalIncome.add(amt.multiply(new BigDecimal(-dir)));
            } else if ("EXPENSE".equals(type)) {
                // 费用：借增贷减
                totalExpense = totalExpense.add(amt.multiply(new BigDecimal(dir)));
            }
        }

        // 计算利润 = 收入 - 费用
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        // 组装数据返回给前端
        result.put("assets", totalAsset);
        result.put("liabilities", totalLiability);
        result.put("equity", totalEquity.add(netProfit)); // 动态权益 = 初始权益 + 本期利润
        result.put("income", totalIncome);
        result.put("expense", totalExpense);
        result.put("profit", netProfit);
        result.put("cashNet", cashInflow.subtract(cashOutflow)); // 净现金流

        return result;
    }
}