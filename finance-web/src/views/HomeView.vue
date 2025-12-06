<script setup>
// 这里模拟一些数据，实际上应该从后端 API 获取 "Dashboard" 数据
import { Wallet, Money, Goods, User } from '@element-plus/icons-vue'
</script>

<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>💰 本月销售额 (收入)</span>
            </div>
          </template>
          <div class="card-value">¥ 128,500.00</div>
          <div class="card-footer">比上月增长 12% 📈</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>💸 本月支出 (成本)</span>
            </div>
          </template>
          <div class="card-value" style="color: #F56C6C;">¥ 45,200.00</div>
          <div class="card-footer">房租/进货/工资</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>🏦 现金及银行余额</span>
            </div>
          </template>
          <div class="card-value" style="color: #67C23A;">¥ 336,593.10</div>
          <div class="card-footer">真金白银流水</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>🧾 待处理单据</span>
            </div>
          </template>
          <div class="card-value" style="color: #E6A23C;">5 笔</div>
          <div class="card-footer">3笔采购待付, 2笔销售待收</div>
        </el-card>
      </el-col>
    </el-row>

    <div style="margin-top: 20px;">
      <el-alert
        title="老板，欢迎回来！"
        description="财务系统已准备就绪。今天是结账日，记得查看利润表。"
        type="success"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<style scoped>
.data-card { height: 180px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; color: #606266; }
.card-value { font-size: 28px; font-weight: bold; margin-top: 20px; color: #303133; }
.card-footer { margin-top: 15px; font-size: 13px; color: #909399; }
</style>