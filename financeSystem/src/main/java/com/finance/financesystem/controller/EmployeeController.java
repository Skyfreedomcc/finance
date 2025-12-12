package com.finance.financesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.financesystem.entity.Employee;
import com.finance.financesystem.entity.FinanceAccount;
import com.finance.financesystem.entity.FinanceSplit;
import com.finance.financesystem.entity.FinanceTransaction;
import com.finance.financesystem.service.IEmployeeService;
import com.finance.financesystem.service.IFinanceAccountService;
import com.finance.financesystem.service.IFinanceSplitService;
import com.finance.financesystem.service.IFinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工管理控制器 (含工资发放逻辑)
 * 更新时间：2025-12-10
 */
@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private IEmployeeService employeeService;

    @Autowired
    private IFinanceAccountService accountService;

    @Autowired
    private IFinanceTransactionService transactionService;

    @Autowired
    private IFinanceSplitService splitService;

    // ================= 原有基础功能 =================

    @GetMapping("/list")
    public List<Employee> list() {
        return employeeService.list();
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Employee employee) {
        return employeeService.saveOrUpdate(employee);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return employeeService.removeById(id);
    }

    // ================= 新增：一键发放工资 =================

    @PostMapping("/payroll")
    @Transactional
    public Map<String, Object> issuePayroll(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取参数
            String month = (String) params.get("month");
            BigDecimal totalAmount = new BigDecimal(params.get("totalAmount").toString());

            // 2. 【关键修复】查找必要的会计科目 (使用代码查找)
            // 1002 = 银行存款, 2211 = 应付职工薪酬
            FinanceAccount bankAccount = accountService.getOne(new QueryWrapper<FinanceAccount>()
                    .eq("account_code", "1002").last("LIMIT 1"));
            FinanceAccount salaryAccount = accountService.getOne(new QueryWrapper<FinanceAccount>()
                    .eq("account_code", "2211").last("LIMIT 1"));

            // 3. 检查科目是否存在
            if (bankAccount == null) {
                throw new RuntimeException("系统未找到【1002 银行存款】科目，请先在基础设置中添加！");
            }
            if (salaryAccount == null) {
                throw new RuntimeException("系统未找到【2211 应付职工薪酬】科目，请先在基础设置中添加！");
            }

            // 4. 创建主凭证
            FinanceTransaction tx = new FinanceTransaction();
            tx.setVoucherDate(LocalDate.now());
            tx.setDescription(month + "月份员工工资发放");
            tx.setStatus("POSTED");
            transactionService.save(tx);
            Long txId = tx.getTransactionId();

            // 5. 创建分录
            // 借：应付职工薪酬
            FinanceSplit splitDebit = new FinanceSplit();
            splitDebit.setTransactionId(txId);
            splitDebit.setAccountId(salaryAccount.getAccountId());
            splitDebit.setSummary(month + " 工资支出");
            splitDebit.setDcDirection(1);
            splitDebit.setAmount(totalAmount);
            splitService.save(splitDebit);

            // 贷：银行存款
            FinanceSplit splitCredit = new FinanceSplit();
            splitCredit.setTransactionId(txId);
            splitCredit.setAccountId(bankAccount.getAccountId());
            splitCredit.setSummary(month + " 工资支出");
            splitCredit.setDcDirection(-1);
            splitCredit.setAmount(totalAmount);
            splitService.save(splitCredit);

            result.put("code", 200);
            result.put("message", "发放成功！已自动生成财务凭证。");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "发放失败：" + e.getMessage());
        }
        return result;
    }
}