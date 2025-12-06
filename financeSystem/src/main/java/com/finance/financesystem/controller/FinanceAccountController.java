package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.service.IFinanceAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/financeAccount")
public class FinanceAccountController {

    @Autowired
    private IFinanceAccountService financeAccountService;

    // 【关键修复】前端下拉框请求的是这个接口
    @GetMapping("/list")
    public List<FinanceAccount> list() {
        // 查询所有科目，并按科目代码排序，这样显示出来整齐
        return financeAccountService.list(new QueryWrapper<FinanceAccount>().orderByAsc("account_code"));
    }

    // 新增科目
    @PostMapping("/add")
    public boolean add(@RequestBody FinanceAccount account) {
        return financeAccountService.save(account);
    }

    // 修改科目
    @PostMapping("/update")
    public boolean update(@RequestBody FinanceAccount account) {
        return financeAccountService.updateById(account);
    }

    // 删除科目
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable Long id) {
        return financeAccountService.removeById(id);
    }
}