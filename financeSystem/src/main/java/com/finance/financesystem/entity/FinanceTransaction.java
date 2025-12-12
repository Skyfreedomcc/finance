package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 交易凭证主表
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("finance_transaction")
public class FinanceTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 凭证ID
     */
    @TableId(value = "transaction_id", type = IdType.AUTO)
    private Long transactionId;

    /**
     * 属于哪个账簿
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 凭证日期
     */
    @TableField("voucher_date")
    private LocalDate voucherDate;

    /**
     * 这笔交易是干嘛的
     */
    @TableField("description")
    private String description;

    /**
     * 录入时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("status")
    private String status;
}
