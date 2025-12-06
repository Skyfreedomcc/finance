package com.finance.financesystem.controller;

import com.finance.financesystem.entity.Customer;
import com.finance.financesystem.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
@CrossOrigin(origins = "*") // 【关键】必须加这行！否则前端点击保存会没反应！
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    // 列表查询接口
    @GetMapping("/list")
    public List<Customer> list() {
        return customerService.list();
    }

    // 保存接口 (新增/修改)
    @PostMapping("/save")
    public boolean save(@RequestBody Customer customer) {
        return customerService.saveOrUpdate(customer);
    }

    // 删除接口
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerService.removeById(id);
    }
}