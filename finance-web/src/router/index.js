import { createRouter, createWebHistory } from 'vue-router'

// 1. 基础页面
import MainLayout from '../views/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'

// 2. 业务页面
import CustomerList from '../views/customer/CustomerList.vue'
import VendorList from '../views/vendor/VendorList.vue'
import EmployeeList from '../views/employee/EmployeeList.vue'
import BillEntry from '../views/invoice/BillEntry.vue'
import ReportView from '../views/report/ReportView.vue'
import AccountList from '../views/account/AccountList.vue'
import UserList from '../views/user/UserList.vue'

// 3. 【新增】刚才做好的工资和凭证页面 (务必确保文件存在)
import PayrollEntry from '../views/employee/PayrollEntry.vue'
import VoucherList from '../views/voucher/VoucherList.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/home', redirect: '/dashboard' },
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: 'dashboard', component: HomeView },

        // === 1. 供应商 & 采购 ===
        { path: 'vendor/list', component: VendorList },
        // 传入 type='PURCHASE'，锁定为采购单
        { path: 'invoice/purchase', component: BillEntry, props: { type: 'PURCHASE' } },

        // === 2. 客户 & 销售 ===
        { path: 'customer/list', component: CustomerList },
        // 传入 type='SALE'，锁定为销售单
        { path: 'invoice/sale', component: BillEntry, props: { type: 'SALE' } },

        // === 3. 员工 & 工资 ===
        { path: 'employee/list', component: EmployeeList },
        // 【关键】工资发放指向新页面
        { path: 'salary/payroll', component: PayrollEntry },

        // === 4. 资金 & 凭证 (新增) ===
        // 【关键】过账查询指向新页面
        { path: 'voucher/list', component: VoucherList },

        // === 5. 报表 ===
        { path: 'report/analysis', component: ReportView },

        // === 6. 设置 ===
        { path: 'account/list', component: AccountList },
        { path: 'user/list', component: UserList }
      ]
    }
  ]
})

export default router