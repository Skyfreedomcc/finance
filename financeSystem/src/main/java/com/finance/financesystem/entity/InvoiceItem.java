package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 发票明细表
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Getter
@Setter
@TableName("finance_invoice_item")
public class InvoiceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /**
     * 归属哪张发票
     */
    @TableField("invoice_id")
    private Long invoiceId;

    /**
     * 商品/服务描述
     */
    @TableField("description")
    private String description;

    /**
     * 数量
     */
    @TableField("quantity")
    private BigDecimal quantity;

    /**
     * 单价
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 行总价 (数量 * 单价)
     */
    @TableField("line_amount")
    private BigDecimal lineAmount;
}