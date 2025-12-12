package com.finance.financesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.financesystem.entity.*;
import com.finance.financesystem.mapper.FinanceSplitMapper;
import com.finance.financesystem.mapper.FinanceTransactionMapper;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceTransactionService;
import com.finance.financesystem.service.IInvoiceService; // 假设你有这个
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinanceTransactionServiceImpl extends ServiceImpl<FinanceTransactionMapper, FinanceTransaction> implements IFinanceTransactionService {

    @Autowired
    private FinanceSplitMapper splitMapper;
    @Autowired
    private IFinanceAccountService accountService;
    // 假设你有 InvoiceService 用来获取单据详情，如果没有，请注入 Mapper
    // @Autowired private IInvoiceService invoiceService; 

    // ================= 高分核心功能 1：自动过账引擎 =================
    /**
     * 将业务单据（Invoice）转换为会计凭证（Transaction + Splits）
     * 解决问题：资产负债表不平、借贷方向错误
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoPostInvoice(Long invoiceId) {
        // 模拟获取单据 (实际开发请取消注释并调用 invoiceService)
        // Invoice invoice = invoiceService.getById(invoiceId);
        // 为了让你代码能跑，我先模拟一个采购单数据，你接好 InvoiceService 后删掉下面这行
        InvoiceMock invoice = new InvoiceMock(invoiceId, "PURCHASE", new BigDecimal("3500.00"), "采购A4纸", LocalDate.now());

        // 1. 创建凭证主头
        FinanceTransaction tx = new FinanceTransaction();
        tx.setBookId(1L); 
        tx.setVoucherDate(invoice.getDate());
        tx.setDescription(invoice.getDesc()); 
        tx.setCreateTime(LocalDateTime.now());
        tx.setStatus("POSTED"); 
        this.save(tx); // 保存主表

        List<FinanceSplit> splits = new ArrayList<>();
        BigDecimal totalAmount = invoice.getAmount();

        // --- 场景 A: 采购 (Purchase) ---
        // 借：库存商品 (资产+) | 贷：应付账款 (负债+)
        if ("PURCHASE".equals(invoice.getType())) {
            // 借方 (Debit = 1)：库存/费用
            FinanceAccount costAcc = findAccountByCode("1405"); // 库存商品
            splits.add(createSplit(tx.getTransactionId(), costAcc.getAccountId(), 1, totalAmount, "采购入库"));

            // 贷方 (Credit = -1)：应付账款
            FinanceAccount payableAcc = findAccountByCode("2202"); // 应付账款
            splits.add(createSplit(tx.getTransactionId(), payableAcc.getAccountId(), -1, totalAmount, "应付供应商"));
        }
        
        // --- 场景 B: 销售 (Sale) ---
        // 借：应收账款 (资产+) | 贷：主营业务收入 (收入+)
        else if ("SALE".equals(invoice.getType())) {
            FinanceAccount receivableAcc = findAccountByCode("1122"); // 应收账款
            splits.add(createSplit(tx.getTransactionId(), receivableAcc.getAccountId(), 1, totalAmount, "应收客户款"));

            FinanceAccount incomeAcc = findAccountByCode("6001"); // 主营业务收入
            splits.add(createSplit(tx.getTransactionId(), incomeAcc.getAccountId(), -1, totalAmount, "销售收入"));
        }

        // 保存分录
        for (FinanceSplit split : splits) {
            splitMapper.insert(split);
        }
        System.out.println("自动过账成功：凭证ID " + tx.getTransactionId());
    }

    // ================= 高分核心功能 2：收付款功能 =================
    /**
     * 支付/收款 (填补 Gap Analysis 中的缺失)
     * type: PAY (付款), RECEIVE (收款)
     */
    @Transactional(rollbackFor = Exception.class)
    public void processPayment(Long relatedInvoiceId, String type, BigDecimal amount) {
        FinanceTransaction tx = new FinanceTransaction();
        tx.setBookId(1L);
        tx.setVoucherDate(LocalDate.now());
        tx.setCreateTime(LocalDateTime.now());
        tx.setStatus("POSTED");
        
        List<FinanceSplit> splits = new ArrayList<>();
        FinanceAccount bankAcc = findAccountByCode("1002"); // 银行存款

        if ("PAY".equals(type)) {
            // 付款：借：应付账款 (负债减少)，贷：银行存款 (资产减少)
            tx.setDescription("支付采购款");
            this.save(tx);
            
            FinanceAccount payableAcc = findAccountByCode("2202");
            // 借 应付 (冲销负债)
            splits.add(createSplit(tx.getTransactionId(), payableAcc.getAccountId(), 1, amount, "冲销应付账款"));
            // 贷 银行 (资金流出)
            splits.add(createSplit(tx.getTransactionId(), bankAcc.getAccountId(), -1, amount, "银行转账支出"));
            
        } else if ("RECEIVE".equals(type)) {
            // 收款：借：银行存款 (资产增加)，贷：应收账款 (资产减少)
            tx.setDescription("收到销售款");
            this.save(tx);
            
            FinanceAccount receivableAcc = findAccountByCode("1122");
            // 借 银行 (资金流入)
            splits.add(createSplit(tx.getTransactionId(), bankAcc.getAccountId(), 1, amount, "收到客户汇款"));
            // 贷 应收 (冲销债权)
            splits.add(createSplit(tx.getTransactionId(), receivableAcc.getAccountId(), -1, amount, "核销应收账款"));
        }

        for (FinanceSplit split : splits) {
            splitMapper.insert(split);
        }
    }

    // --- 辅助工具方法 ---
    private FinanceSplit createSplit(Long txId, Long accId, int dir, BigDecimal amt, String summary) {
        FinanceSplit s = new FinanceSplit();
        s.setTransactionId(txId);
        s.setAccountId(accId);
        s.setDcDirection(dir);
        s.setAmount(amt);
        s.setSummary(summary);
        return s;
    }

    private FinanceAccount findAccountByCode(String code) {
        QueryWrapper<FinanceAccount> q = new QueryWrapper<>();
        q.eq("account_code", code);
        return accountService.getOne(q);
    }

    // 内部模拟类，防止你还没写 Invoice 实体报错
    class InvoiceMock {
        Long id; String type; BigDecimal amount; String desc; LocalDate date;
        public InvoiceMock(Long id, String t, BigDecimal a, String d, LocalDate dt) {this.id=id;this.type=t;this.amount=a;this.desc=d;this.date=dt;}
        public BigDecimal getAmount() {return amount;}
        public String getType() {return type;}
        public String getDesc() {return desc;}
        public LocalDate getDate() {return date;}
    }
}