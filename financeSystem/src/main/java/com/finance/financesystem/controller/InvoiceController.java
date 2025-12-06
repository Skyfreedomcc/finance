package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.financesystem.entity.Invoice;
import com.finance.financesystem.service.IInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 发票管理 API
 * </p>
 */
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    @Autowired
    private IInvoiceService invoiceService;

    @GetMapping("/page")
    public Page<Invoice> page(@RequestParam(defaultValue = "1") int page, 
                              @RequestParam(defaultValue = "10") int size) {
        return invoiceService.page(new Page<>(page, size));
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Invoice invoice) {
        return invoiceService.saveOrUpdate(invoice);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return invoiceService.removeById(id);
    }
}