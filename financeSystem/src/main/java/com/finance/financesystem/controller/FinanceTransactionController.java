package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.service.IFinanceSplitService;
import com.finance.financesystem.service.IFinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/financeTransaction")
@CrossOrigin(origins = "*")
public class FinanceTransactionController {

    @Autowired
    private IFinanceTransactionService transactionService;

    @Autowired
    private IFinanceSplitService splitService;

    // --- DTO: 用于接收前端保存的数据 ---
    static class TransactionDTO extends FinanceTransaction {
        private List<FinanceSplit> splits;
        public List<FinanceSplit> getSplits() { return splits; }
        public void setSplits(List<FinanceSplit> splits) { this.splits = splits; }
    }

    // --- VO: 用于展示给前端的数据 (包含总金额) ---
    static class TransactionVO extends FinanceTransaction {
        private BigDecimal totalAmount; // 额外加一个金额字段
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    /**
     * 【新增接口】查询凭证列表
     * 自动计算每张凭证的总金额
     */
    @GetMapping("/list")
    public List<TransactionVO> list() {
        // 1. 查出所有凭证头
        List<FinanceTransaction> transactions = transactionService.list(new QueryWrapper<FinanceTransaction>().orderByDesc("voucher_date"));
        
        List<TransactionVO> resultList = new ArrayList<>();

        // 2. 遍历凭证，去分录表查金额
        for (FinanceTransaction tx : transactions) {
            TransactionVO vo = new TransactionVO();
            // 复制属性
            vo.setTransactionId(tx.getTransactionId());
            vo.setVoucherDate(tx.getVoucherDate());
            vo.setDescription(tx.getDescription());
            
            // 3. 查该凭证下的所有分录
            List<FinanceSplit> splits = splitService.list(new QueryWrapper<FinanceSplit>().eq("transaction_id", tx.getTransactionId()));
            
            // 4. 计算金额 (把借方金额加起来作为总金额)
            BigDecimal total = BigDecimal.ZERO;
            for (FinanceSplit split : splits) {
                if (split.getDcDirection() == 1) { // 1代表借方
                    total = total.add(split.getAmount());
                }
            }
            vo.setTotalAmount(total);
            resultList.add(vo);
        }
        return resultList;
    }

    /**
     * 保存凭证 (工资、采购、销售都用这个)
     */
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addTransaction(@RequestBody TransactionDTO dto) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 保存头
        dto.setCreateTime(LocalDateTime.now());
        transactionService.save(dto);

        // 2. 保存分录
        Long transId = dto.getTransactionId();
        for (FinanceSplit split : dto.getSplits()) {
            split.setTransactionId(transId);
            if(split.getDcDirection() == null) split.setDcDirection(1);
            if(split.getReconcileState() == null) split.setReconcileState("n");
        }
        splitService.saveBatch(dto.getSplits());

        result.put("code", 200);
        result.put("msg", "凭证保存成功");
        return result;
    }
}