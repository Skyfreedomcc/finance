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
 * 往来单位表 (客户/供应商)
 * </p>
 *
 * @author AI_Assistant
 * @since 2025-12-02
 */
@Getter
@Setter
@TableName("finance_customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "customer_id", type = IdType.AUTO)
    private Long customerId;

    /**
     * 单位名称
     */
    @TableField("name")
    private String name;

    /**
     * 类型：CUSTOMER(客户), VENDOR(供应商)
     */
    @TableField("type")
    private String type;

    /**
     * 联系人
     */
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    @TableField("create_time")
    private LocalDateTime createTime;
}