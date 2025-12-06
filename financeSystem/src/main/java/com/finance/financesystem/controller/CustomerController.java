package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.financesystem.entity.Customer;
import com.finance.financesystem.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 往来单位管理 API
 * </p>
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    // 获取所有列表
    @GetMapping("/list")
    public List<Customer> list() {
        return customerService.list();
    }

    // 分页查询 (前端表格用)
    @GetMapping("/page")
    public Page<Customer> page(@RequestParam(defaultValue = "1") int page, 
                               @RequestParam(defaultValue = "10") int size) {
        return customerService.page(new Page<>(page, size));
    }

    // 新增或保存
    @PostMapping("/save")
    public boolean save(@RequestBody Customer customer) {
        return customerService.saveOrUpdate(customer);
    }

    // 删除
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerService.removeById(id);
    }
}