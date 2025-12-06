package com.finance.financesystem.controller;

import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.service.IFinanceSplitService;
import com.finance.financesystem.service.IFinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/financeTransaction")
@CrossOrigin(origins = "*")
public class FinanceTransactionController {

    @Autowired
    private IFinanceTransactionService transactionService; // 管凭证头的

    @Autowired
    private IFinanceSplitService splitService; // 管分录行的

    // 接收前端数据的“包裹”类
    // 前端发来的 JSON 长这样： { "bookId": 1, "voucherDate": "...", "description": "...", "splits": [ ... ] }
    static class TransactionDTO extends FinanceTransaction {
        private List<FinanceSplit> splits; // 多条分录
        public List<FinanceSplit> getSplits() { return splits; }
        public void setSplits(List<FinanceSplit> splits) { this.splits = splits; }
    }

    /**
     * 【核心】保存凭证接口
     * 加上 @Transactional 注解，保证"同生共死"
     */
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class) // 如果报错，全部回滚，不留脏数据
    public Map<String, Object> addTransaction(@RequestBody TransactionDTO dto) {
        Map<String, Object> result = new HashMap<>();

        // 1. 简单的借贷平衡检查
        double debitTotal = 0;
        double creditTotal = 0;
        for (FinanceSplit split : dto.getSplits()) {
            if (split.getDcDirection() == 1) { // 借
                debitTotal += split.getAmount().doubleValue();
            } else { // 贷
                creditTotal += split.getAmount().doubleValue();
            }
        }

        // 允许 0.01 的误差
        if (Math.abs(debitTotal - creditTotal) > 0.01) {
            result.put("code", 400);
            result.put("msg", "借贷不平衡！借方:" + debitTotal + " 贷方:" + creditTotal);
            return result;
        }

        // 2. 先保存“凭证主表”
        dto.setCreateTime(LocalDateTime.now());
        transactionService.save(dto);
        // 此时 dto.getTransactionId() 已经被自动生成了

        // 3. 再保存“分录表”
        Long transId = dto.getTransactionId();
        for (FinanceSplit split : dto.getSplits()) {
            split.setTransactionId(transId); // 把分录和主表关联起来
        }
        splitService.saveBatch(dto.getSplits()); // 批量保存分录

        result.put("code", 200);
        result.put("msg", "凭证保存成功！");
        return result;
    }
}