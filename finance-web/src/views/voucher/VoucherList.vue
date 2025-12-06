<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    // 【关键】这里现在调用的是真实的后端接口
    const res = await axios.get('http://localhost:8080/financeTransaction/list')
    list.value = res.data || []
  } catch (err) {
    console.error(err)
    ElMessage.error('无法加载凭证，请检查后端是否启动')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h3>凭证查询</h3>
      <el-button type="primary" @click="loadData">刷新列表</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading" style="margin-top: 20px;">
      <el-table-column prop="transactionId" label="凭证号" width="100" align="center" />
      <el-table-column prop="voucherDate" label="日期" width="150" sortable />
      <el-table-column prop="description" label="摘要 (业务描述)" min-width="200" />

      <el-table-column prop="totalAmount" label="总金额" width="180" align="right">
        <template #default="scope">
          <strong style="color: #409EFF;">¥ {{ scope.row.totalAmount }}</strong>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="s"><el-tag type="success">已过账</el-tag></template>
      </el-table-column>
    </el-table>

    <template v-if="!loading && list.length === 0">
      <div style="text-align:center; padding: 40px; color:#999">
        暂无凭证记录，请先去【业务发票】或【工资发放】录入数据。
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-container { background: #fff; padding: 20px; border-radius: 8px; }
.header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; padding-bottom: 15px;}
</style>