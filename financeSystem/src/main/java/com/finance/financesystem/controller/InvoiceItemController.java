package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.InvoiceItem;
import com.finance.financesystem.service.IInvoiceItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 发票明细 API
 * </p>
 */
@RestController
@RequestMapping("/invoice-item")
public class InvoiceItemController {

    @Autowired
    private IInvoiceItemService invoiceItemService;

    // 根据发票ID查询所有明细
    @GetMapping("/by-invoice/{invoiceId}")
    public List<InvoiceItem> listByInvoice(@PathVariable Long invoiceId) {
        return invoiceItemService.list(new QueryWrapper<InvoiceItem>().eq("invoice_id", invoiceId));
    }

    @PostMapping("/save")
    public boolean save(@RequestBody InvoiceItem item) {
        return invoiceItemService.saveOrUpdate(item);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return invoiceItemService.removeById(id);
    }
}