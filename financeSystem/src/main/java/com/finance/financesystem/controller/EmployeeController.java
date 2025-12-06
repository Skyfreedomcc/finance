package com.finance.financesystem.controller;

import com.finance.financesystem.entity.Employee;
import com.finance.financesystem.service.IEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理控制器
 */
@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "*") // 【关键】允许前端跨域访问，必须有这一行！
public class EmployeeController {

    @Autowired
    private IEmployeeService employeeService;

    // 获取所有员工列表
    @GetMapping("/list")
    public List<Employee> list() {
        return employeeService.list();
    }

    // 新增或修改员工
    @PostMapping("/save")
    public boolean save(@RequestBody Employee employee) {
        return employeeService.saveOrUpdate(employee);
    }

    // 删除员工
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return employeeService.removeById(id);
    }
}