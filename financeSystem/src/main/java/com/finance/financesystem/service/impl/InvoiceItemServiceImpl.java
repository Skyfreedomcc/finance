package com.finance.financesystem.service.impl;

import com.finance.financesystem.entity.InvoiceItem;
import com.finance.financesystem.mapper.InvoiceItemMapper;
import com.finance.financesystem.service.IInvoiceItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 发票明细 服务实现类
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Service
public class InvoiceItemServiceImpl extends ServiceImpl<InvoiceItemMapper, InvoiceItem> implements IInvoiceItemService {
}