package com.finance.financesystem.service.impl;

import com.finance.financesystem.entity.Invoice;
import com.finance.financesystem.mapper.InvoiceMapper;
import com.finance.financesystem.service.IInvoiceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 发票 服务实现类
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements IInvoiceService {
}