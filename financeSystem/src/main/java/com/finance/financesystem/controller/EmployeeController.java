package com.finance.financesystem.controller;

import com.finance.financesystem.entity.Employee;
import com.finance.financesystem.service.IEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee") // 前端就是请求这个路径
public class EmployeeController {

    @Autowired
    private IEmployeeService employeeService;

    // 前端请求 GET /employee/list 就是找这里
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