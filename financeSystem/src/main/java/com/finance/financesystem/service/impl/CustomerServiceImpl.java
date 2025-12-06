package com.finance.financesystem.service.impl;

import com.finance.financesystem.entity.Customer;
import com.finance.financesystem.mapper.CustomerMapper;
import com.finance.financesystem.service.ICustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 往来单位 服务实现类
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {
}