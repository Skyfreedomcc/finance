package com.finance.financesystem.controller;

import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.service.IFinanceAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/financeAccount")
@CrossOrigin(origins = "*")
public class FinanceAccountController {

    @Autowired
    private IFinanceAccountService accountService;

    /**
     * 获取所有会计科目列表
     */
    @GetMapping("/list")
    public List<FinanceAccount> list() {
        return accountService.list();
    }

    /**
     * 获取树形结构的科目表
     */
    @GetMapping("/tree")
    public List<FinanceAccount> tree() {
        // 返回所有科目，前端可以自己构建树
        return accountService.list();
    }

    /**
     * 根据ID获取单个科目
     */
    @GetMapping("/{id}")
    public FinanceAccount getById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    /**
     * 新增科目
     */
    @PostMapping
    public String add(@RequestBody FinanceAccount account) {
        accountService.save(account);
        return "新增成功";
    }

    /**
     * 更新科目
     */
    @PutMapping
    public String update(@RequestBody FinanceAccount account) {
        accountService.updateById(account);
        return "更新成功";
    }

    /**
     * 删除科目
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        accountService.removeById(id);
        return "删除成功";
    }
}