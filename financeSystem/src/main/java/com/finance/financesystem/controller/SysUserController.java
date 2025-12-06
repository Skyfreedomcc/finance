package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.financesystem.entity.SysUser;
import com.finance.financesystem.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/sysUser")
@CrossOrigin(origins = "*") // 【关键！】允许所有前端访问（解决跨域问题）
public class SysUserController {

    @Autowired
    private ISysUserService sysUserService;

    // 测试接口
    @GetMapping("/hello")
    public String hello() {
        return "Hello! 系统启动成功了！";
    }

    // 查询所有用户
    @GetMapping("/list")
    public List<SysUser> getAllUsers() {
        return sysUserService.list();
    }
    @PostMapping("/add")
    public Map<String, Object> addUser(@RequestBody SysUser user) {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查用户名是否已存在
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));

        if (count > 0) {
            result.put("code", 400);
            result.put("msg", "用户名已存在，换一个吧！");
            return result;
        }

        // 2. 存入数据库
        // (为了简单，我们暂时给没填密码的用户设置默认密码 123456)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }

        boolean success = sysUserService.save(user);

        if (success) {
            result.put("code", 200);
            result.put("msg", "添加成功");
        } else {
            result.put("code", 500);
            result.put("msg", "添加失败");
        }
        return result;
    }

    /**
     * 【新增】登录接口
     * 接收前端传来的 JSON 数据 (包含 username 和 password)
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody SysUser loginUser) {
        Map<String, Object> result = new HashMap<>();

        // 1. 去数据库查这个用户名
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginUser.getUsername()));

        // 2. 判断用户是否存在
        if (user == null) {
            result.put("code", 400);
            result.put("msg", "用户不存在");
            return result;
        }

        // 3. 判断密码是否正确 (这里演示暂时用明文比对，实际开发要加密)
        if (!user.getPassword().equals(loginUser.getPassword())) {
            result.put("code", 400);
            result.put("msg", "密码错误");
            return result;
        }

        // 4. 登录成功
        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", user); // 把用户信息返给前端
        return result;
    }
}