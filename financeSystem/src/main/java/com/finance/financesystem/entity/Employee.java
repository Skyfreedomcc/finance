package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("finance_employee")
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "employee_id", type = IdType.AUTO)
    private Long employeeId;

    @TableField("name")
    private String name;

    @TableField("department") // 部门
    private String department;

    @TableField("position") // 职位
    private String position;

    @TableField("phone")
    private String phone;
    
    @TableField("basic_salary") // 基本工资 (用于发工资时自动带出)
    private BigDecimal basicSalary;
}