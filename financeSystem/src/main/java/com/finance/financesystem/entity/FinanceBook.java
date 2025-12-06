package com.finance.financesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 账簿表
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("finance_book")
public class FinanceBook implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账簿ID
     */
    @TableId(value = "book_id", type = IdType.AUTO)
    private Long bookId;

    /**
     * 账簿名称，比如“2025年总账”
     */
    @TableField("book_name")
    private String bookName;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
