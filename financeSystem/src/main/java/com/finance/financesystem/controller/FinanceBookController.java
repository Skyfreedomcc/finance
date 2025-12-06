package com.finance.financesystem.controller;

import com.finance.financesystem.entity.FinanceBook;
import com.finance.financesystem.service.IFinanceBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账簿管理控制器
 */
@RestController
@RequestMapping("/financeBook") // 注意：前端请求路径是 /financeBook/...
@CrossOrigin(origins = "*")     // 允许前端跨域访问
public class FinanceBookController {

    @Autowired
    private IFinanceBookService financeBookService;

    // 1. 获取所有账簿列表
    @GetMapping("/list")
    public List<FinanceBook> getAllBooks() {
        return financeBookService.list();
    }

    // 2. 新增账簿
    @PostMapping("/add")
    public Map<String, Object> addBook(@RequestBody FinanceBook book) {
        Map<String, Object> result = new HashMap<>();

        // 简单校验
        if (book.getBookName() == null || book.getBookName().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "账簿名称不能为空");
            return result;
        }

        // 保存到数据库
        boolean success = financeBookService.save(book);

        if (success) {
            result.put("code", 200);
            result.put("msg", "创建账簿成功");
        } else {
            result.put("code", 500);
            result.put("msg", "创建失败");
        }
        return result;
    }
}