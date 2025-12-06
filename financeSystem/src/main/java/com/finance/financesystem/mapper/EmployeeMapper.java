package com.finance.financesystem.mapper;

import com.finance.financesystem.entity.Employee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    // 继承 BaseMapper 后，自动拥有增删改查能力，无需写 XML
}