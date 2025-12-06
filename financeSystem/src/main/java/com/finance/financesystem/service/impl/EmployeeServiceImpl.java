package com.finance.financesystem.service.impl;

import com.finance.financesystem.entity.Employee;
import com.finance.financesystem.mapper.EmployeeMapper;
import com.finance.financesystem.service.IEmployeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements IEmployeeService {}