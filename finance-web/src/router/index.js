import { createRouter, createWebHistory } from 'vue-router'

// 1. 基础页面
import MainLayout from '../views/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'

// 2. 业务页面 (原有的)
import CustomerList from '../views/customer/CustomerList.vue'
import VendorList from '../views/vendor/VendorList.vue'
import EmployeeList from '../views/employee/EmployeeList.vue'
import BillEntry from '../views/invoice/BillEntry.vue'
import ReportView from '../views/report/ReportView.vue'
import AccountList from '../views/account/AccountList.vue'
import UserList from '../views/user/UserList.vue'
import PayrollEntry from '../views/employee/PayrollEntry.vue'
import VoucherList from '../views/voucher/VoucherList.vue'

// 3. 【新增】引入新开发的页面
import PostingView from '../views/voucher/PostingView.vue'      // 过账中心
import SubjectLedger from '../views/ledger/SubjectLedger.vue'   // 科目余额表(树形日记账)

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

        // === 1. 资金 & 凭证管理 ===
        // 原始的流水账
        { path: 'voucher/list', component: VoucherList },
        // 【新增】过账中心
        { path: 'voucher/posting', component: PostingView },
        // 【新增】科目余额表 (仿GnuCash树形账)
        { path: 'ledger/subject', component: SubjectLedger },
        {
          path: '/voucher/entry',
          name: 'VoucherEntry',
          component: () => import('@/views/voucher/VoucherEntry.vue')
        },
        // 【新增】科目明细穿透页 (动态路由)
        {
          path: '/ledger/:id',
          name: 'AccountLedger',
          component: () => import('../views/account/AccountLedger.vue')
        },

        // === 2. 供应商 & 采购 ===
        { path: 'vendor/list', component: VendorList },
        { path: 'invoice/purchase', component: BillEntry, props: { type: 'PURCHASE' } },

        // === 3. 客户 & 销售 ===
        { path: 'customer/list', component: CustomerList },
        { path: 'invoice/sale', component: BillEntry, props: { type: 'SALE' } },

        // === 4. 员工 & 工资 ===
        { path: 'employee/list', component: EmployeeList },
        { path: 'salary/payroll', component: PayrollEntry },

        // === 5. 报表 ===
        { path: 'report/analysis', component: ReportView },

        // === 6. 设置 ===
        { path: 'account/list', component: AccountList },
        { path: 'user/list', component: UserList },
      ]
    }
  ]
})

export default router