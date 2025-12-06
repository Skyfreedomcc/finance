package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 业务发票主表
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Getter
@Setter
@TableName("finance_invoice")
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "invoice_id", type = IdType.AUTO)
    private Long invoiceId;

    /**
     * 关联的客户/供应商ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 发票编号 (如: INV-2025001)
     */
    @TableField("invoice_code")
    private String invoiceCode;

    /**
     * 业务类型：SALE(销售/应收), PURCHASE(采购/应付)
     */
    @TableField("type")
    private String type;

    /**
     * 开票日期
     */
    @TableField("invoice_date")
    private LocalDate invoiceDate;

    /**
     * 到期日期
     */
    @TableField("due_date")
    private LocalDate dueDate;

    /**
     * 总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：DRAFT(草稿), POSTED(已入账), PAID(已支付)
     */
    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;
}