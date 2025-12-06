package com.finance.financesystem.mapper;

import com.finance.financesystem.entity.Invoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 发票 Mapper 接口
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}