<script setup>
import { useRouter } from 'vue-router'
import {
  Menu as IconMenu, PieChart, Setting,
  Avatar, Box, Van, Wallet, DocumentChecked, Reading,
  CreditCard
} from '@element-plus/icons-vue'

const router = useRouter()
const handleLogout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<template>
  <div class="common-layout">
    <el-container>
      <el-aside width="220px" class="aside-menu">

        <div class="logo-box"><h2>💰 企业财务通</h2></div>

        <el-menu
          class="custom-menu"
          :default-active="$route.path"
          router
          background-color="#001529"
          text-color="#a6adb4"
          active-text-color="#409EFF"
        >

          <el-menu-item index="/dashboard">
            <el-icon><IconMenu /></el-icon><span>工作台</span>
          </el-menu-item>

          <el-sub-menu index="1">
            <template #title><el-icon><Wallet /></el-icon><span>资金账务</span></template>
            <el-menu-item index="/voucher/list">
              <el-icon><Reading /></el-icon><span>凭证序时簿 (流水)</span>
            </el-menu-item>
            <el-menu-item index="/ledger/subject">
              <el-icon><Wallet /></el-icon><span>科目余额表 (总账)</span>
            </el-menu-item>
            <el-menu-item index="/voucher/posting">
              <el-icon><DocumentChecked /></el-icon><span>过账审核中心</span>
            </el-menu-item>
            <el-menu-item index="/payment/center">
              <el-icon><CreditCard /></el-icon><span>收付款中心</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="2">
            <template #title><el-icon><Van /></el-icon><span>供应商 (采购)</span></template>
            <el-menu-item index="/vendor/list">供应商列表</el-menu-item>
            <el-menu-item index="/invoice/purchase">新建采购账单</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="3">
            <template #title><el-icon><Box /></el-icon><span>客户 (销售)</span></template>
            <el-menu-item index="/customer/list">客户列表</el-menu-item>
            <el-menu-item index="/invoice/sale">新建销售发票</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="4">
            <template #title><el-icon><Avatar /></el-icon><span>员工管理</span></template>
            <el-menu-item index="/employee/list">员工花名册</el-menu-item>
            <el-menu-item index="/salary/payroll">工资发放</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/report/analysis">
            <el-icon><PieChart /></el-icon><span>财务报表中心</span>
          </el-menu-item>

          <el-sub-menu index="6">
            <template #title><el-icon><Setting /></el-icon><span>基础设置</span></template>
            <el-menu-item index="/account/list">会计科目表</el-menu-item>
            <el-menu-item index="/user/list">用户权限</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header-bar">
          <div>当前账套：2025年主账簿 (默认)</div>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </el-header>
        <el-main class="main-content"><RouterView /></el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
/* 确保最外层容器占满全屏 */
.common-layout, .el-container {
  height: 100vh;
  overflow: hidden; /* 防止出现双重滚动条 */
}

/* 侧边栏整体样式 */
.aside-menu {
  background-color: #001529;
  /* 禁止侧边栏本身滚动 */
  overflow: hidden !important;
  height: 100vh;
  position: relative;
  /* 如果你的 Element Plus 版本导致宽度不对，可以尝试强制 flex-shrink */
  flex-shrink: 0;
}

/* Logo 区域：固定高度 */
.logo-box {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: white;
  background: #002140;
  width: 100%;
  position: absolute; /* 绝对定位在顶部 */
  top: 0;
  left: 0;
  z-index: 100; /* 确保在菜单之上 */
}

/* 菜单区域：关键修改 */
.custom-menu {
  /* 1. 强行设置高度 = 屏幕高度(100vh) - Logo高度(60px)
     2. 加上 margin-top: 60px 给 Logo 让出位置
  */
  height: calc(100vh - 60px) !important;
  margin-top: 60px;

  /* 开启垂直滚动 */
  overflow-y: auto !important;

  /* 去掉边框 */
  border-right: none;

  /* 确保宽度占满 */
  width: 100%;
}

/* 滚动条样式美化 */
.custom-menu::-webkit-scrollbar {
  width: 6px;
}
.custom-menu::-webkit-scrollbar-thumb {
  background: #3c495e;
  border-radius: 3px;
}
.custom-menu::-webkit-scrollbar-track {
  background: #001529;
}

.header-bar {
  background: #fff;
  border-bottom: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>