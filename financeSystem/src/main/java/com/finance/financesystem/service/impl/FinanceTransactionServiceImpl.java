package com.finance.financesystem.service.impl;

import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.mapper.FinanceTransactionMapper;
import com.finance.financesystem.service.IFinanceTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 交易凭证主表 服务实现类
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-11-26
 */
@Service
public class FinanceTransactionServiceImpl extends ServiceImpl<FinanceTransactionMapper, FinanceTransaction> implements IFinanceTransactionService {

}
