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
 * 交易分录表 (升级版)
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("finance_split")
public class FinanceSplit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "split_id", type = IdType.AUTO)
    private Long splitId;

    @TableField("transaction_id")
    private Long transactionId;

    @TableField("account_id")
    private Long accountId;

    /**
     * 方向：1是借(Dr)，-1是贷(Cr)
     */
    @TableField("dc_direction")
    private Integer dcDirection;

    @TableField("amount")
    private BigDecimal amount;

    /**
     * 【新增】对账状态：'n'=未对账, 'c'=已结清, 'y'=已对账
     */
    @TableField("reconcile_state")
    private String reconcileState;

    @TableField("summary")
    private String summary;
}