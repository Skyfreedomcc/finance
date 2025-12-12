package com.finance.financesystem.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于返回给前端的“树形”会计科目
 * 包含：当前科目的余额 + 子科目列表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountTreeVO {
    private Long id;              // 科目ID
    private String name;          // 科目名称 (如：货币资金)
    private String code;          // 科目代码 (如：1001)
    private String type;          // 类型 (ASSET/LIABILITY...)
    private String direction;     // 余额方向 (DEBIT/CREDIT)
    private BigDecimal amount;    // 当前余额 (如果是父节点，则是子节点之和)
    private List<AccountTreeVO> children = new ArrayList<>(); // 子科目列表

    // 构造函数
    public AccountTreeVO(Long id, String name, String code, String type, String direction) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.direction = direction;
        this.amount = BigDecimal.ZERO;
    }
}