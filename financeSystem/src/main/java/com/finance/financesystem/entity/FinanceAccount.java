package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 会计科目表 (GnuCash 风格升级版)
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("finance_account")
public class FinanceAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科目ID
     */
    @TableId(value = "account_id", type = IdType.AUTO)
    private Long accountId;

    /**
     * 属于哪个账簿
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 【新增】父科目ID，实现树形结构 (比如：银行存款 -> 工商银行)
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 科目名称
     */
    @TableField("account_name")
    private String accountName;

    /**
     * 类型：ASSET(资产), LIABILITY(负债), EQUITY(权益), INCOME(收入), EXPENSE(费用)
     */
    @TableField("account_type")
    private String accountType;

    /**
     * 科目代码，比如 1001
     */
    @TableField("account_code")
    private String accountCode;

    /**
     * 【新增】余额方向：DEBIT(借方增加) / CREDIT(贷方增加)
     */
    @TableField("balance_direction")
    private String balanceDirection;

    /**
     * 备注描述
     */
    @TableField("description")
    private String description;
}