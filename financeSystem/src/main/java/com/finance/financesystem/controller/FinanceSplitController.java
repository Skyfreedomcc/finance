package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分录控制器
 * 
 * 提供分录查询接口，用于凭证流水展示
 */
@RestController
@RequestMapping("/financeSplit")
@CrossOrigin(origins = "*")
public class FinanceSplitController {

    @Autowired
    private IFinanceSplitService splitService;
    
    @Autowired
    private IFinanceAccountService accountService;

    /**
     * 根据凭证ID获取分录列表
     * 
     * @param transactionId 凭证ID
     * @return 分录列表（包含科目名称）
     */
    @GetMapping("/list")
    public List<Map<String, Object>> getList(@RequestParam(required = false) Long transactionId) {
        QueryWrapper<FinanceSplit> qw = new QueryWrapper<>();
        
        if (transactionId != null) {
            qw.eq("transaction_id", transactionId);
        }
        
        qw.orderByAsc("split_id");
        List<FinanceSplit> splits = splitService.list(qw);
        
        // 获取所有科目信息
        List<FinanceAccount> accounts = accountService.list();
        Map<Long, String> accountNameMap = accounts.stream()
            .collect(Collectors.toMap(
                FinanceAccount::getAccountId, 
                FinanceAccount::getAccountName,
                (a, b) -> a
            ));
        
        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (FinanceSplit split : splits) {
            Map<String, Object> item = new HashMap<>();
            item.put("splitId", split.getSplitId());
            item.put("transactionId", split.getTransactionId());
            item.put("accountId", split.getAccountId());
            item.put("accountName", accountNameMap.getOrDefault(split.getAccountId(), "未知科目"));
            item.put("dcDirection", split.getDcDirection());
            item.put("amount", split.getAmount());
            item.put("summary", split.getSummary());
            item.put("reconcileState", split.getReconcileState());
            
            result.add(item);
        }
        
        return result;
    }

    /**
     * 获取指定科目的所有分录
     * 
     * @param accountId 科目ID
     * @return 分录列表
     */
    @GetMapping("/byAccount/{accountId}")
    public List<Map<String, Object>> getByAccount(@PathVariable Long accountId) {
        QueryWrapper<FinanceSplit> qw = new QueryWrapper<>();
        qw.eq("account_id", accountId);
        qw.orderByAsc("split_id");
        
        List<FinanceSplit> splits = splitService.list(qw);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (FinanceSplit split : splits) {
            Map<String, Object> item = new HashMap<>();
            item.put("splitId", split.getSplitId());
            item.put("transactionId", split.getTransactionId());
            item.put("accountId", split.getAccountId());
            item.put("dcDirection", split.getDcDirection());
            item.put("amount", split.getAmount());
            item.put("summary", split.getSummary());
            
            result.add(item);
        }
        
        return result;
    }
}