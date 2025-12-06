<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const list = ref([])

const loadData = async () => {
  // 我们需要一个新的后端接口来查凭证，暂时假设有 (你需要去后端加一个 FinanceTransactionController.list())
  // 暂时用前端模拟数据展示效果
  list.value = [
    { id: 1, date: '2025-12-06', desc: '销售发票', amount: 5000, status: '已过账' },
    { id: 2, date: '2025-12-06', desc: '采购办公用品', amount: 200, status: '已过账' }
  ]
}
onMounted(() => loadData())
</script>

<template>
  <div style="background:white; padding:20px;">
    <h3>📖 凭证序时簿 (日记账)</h3>
    <p style="color:gray; font-size:12px;">此处列出所有已过账的会计凭证。</p>

    <el-table :data="list" border stripe>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="desc" label="摘要" />
      <el-table-column prop="amount" label="金额" width="150" align="right">
        <template #default="s">¥ {{ s.row.amount.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="s"><el-tag type="success">已过账</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>