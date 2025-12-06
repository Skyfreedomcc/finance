package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.service.IFinanceAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计科目控制器
 */
@RestController
@RequestMapping("/financeAccount")
@CrossOrigin(origins = "*") // 【绝对关键】必须加这行，否则前端拿不到数据！
public class FinanceAccountController {

    @Autowired
    private IFinanceAccountService financeAccountService;

    /**
     * 获取科目列表 (前端下拉框和列表页都用这个)
     */
    @GetMapping("/list")
    public List<FinanceAccount> list() {
        // 查询所有科目，并按照科目代码 (1001, 1002...) 正序排列
        return financeAccountService.list(new QueryWrapper<FinanceAccount>().orderByAsc("account_code"));
    }

    /**
     * 新增科目
     */
    @PostMapping("/add")
    public boolean add(@RequestBody FinanceAccount account) {
        return financeAccountService.save(account);
    }

    /**
     * 修改科目
     */
    @PostMapping("/update")
    public boolean update(@RequestBody FinanceAccount account) {
        return financeAccountService.updateById(account);
    }

    /**
     * 删除科目
     */
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable Long id) {
        return financeAccountService.removeById(id);
    }
}