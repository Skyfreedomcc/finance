<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const month = ref(new Date().toISOString().slice(0, 7))
const employeeList = ref([])
const loading = ref(false)

// 1. Load Data with Error Handling
const loadData = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/employee/list')
    // Handle empty data gracefully
    const rawData = res.data || []

    employeeList.value = rawData.map(emp => ({
      ...emp,
      bonus: 0,
      deduction: 0,
      realSalary: emp.basicSalary || 0
    }))
  } catch (err) {
    console.error("Failed to load employees:", err)
    // Optional: Only show error if it's NOT a 404 (which might mean just no data)
    ElMessage.warning('暂无员工数据或无法连接后端')
  } finally {
    // CRITICAL: Stop the spinner no matter what!
    loading.value = false
  }
}

// 2. Real-time Calculation
const calculateTotal = (row) => {
  row.realSalary = Number(row.basicSalary || 0) + Number(row.bonus || 0) - Number(row.deduction || 0)
}

// 3. Total Calculation
const totalPayroll = computed(() => {
  return employeeList.value.reduce((sum, item) => sum + item.realSalary, 0)
})

// 4. Submit Payroll (Auto-Posting)
const submitPayroll = async () => {
  if (employeeList.value.length === 0) return ElMessage.warning('没有员工可以发工资')

  ElMessageBox.confirm(`本月预计发放工资 ¥${totalPayroll.value}，确定吗？`, '发放确认', {
    confirmButtonText: '确认发放并记账',
    type: 'warning'
  }).then(async () => {
    const transaction = {
      description: `${month.value} 员工工资发放`,
      voucherDate: new Date().toISOString().split('T')[0],
      splits: [
        { accountId: 26, summary: '工资支出', dcDirection: 1, amount: totalPayroll.value },
        { accountId: 7, summary: '银行代发', dcDirection: -1, amount: totalPayroll.value }
      ]
    }

    try {
      await axios.post('http://localhost:8080/financeTransaction/add', transaction)
      ElMessage.success('工资已发放，系统自动完成过账！')
    } catch (e) {
      ElMessage.error('发放失败，请检查会计科目ID是否存在')
    }
  })
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h3>💰 员工薪酬发放</h3>
      <div class="actions">
        <span>计薪月份：</span>
        <el-date-picker v-model="month" type="month" :clearable="false" style="width: 150px; margin-right: 15px;" />
        <el-button type="primary" @click="submitPayroll">一键发放 (¥{{ totalPayroll }})</el-button>
      </div>
    </div>

    <el-table
      :data="employeeList"
      border stripe
      v-loading="loading"
      element-loading-text="正在加载员工列表..."
    >
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="position" label="职位" width="120" />
      <el-table-column prop="basicSalary" label="基本工资" width="120" />

      <el-table-column label="本月奖金 (+)">
        <template #default="scope">
          <el-input-number v-model="scope.row.bonus" :min="0" :step="100" @change="calculateTotal(scope.row)" />
        </template>
      </el-table-column>

      <el-table-column label="本月扣款 (-)">
        <template #default="scope">
          <el-input-number v-model="scope.row.deduction" :min="0" :step="50" @change="calculateTotal(scope.row)" />
        </template>
      </el-table-column>

      <el-table-column label="实发工资" fixed="right" width="150">
        <template #default="scope">
          <strong style="color: #67C23A;">¥ {{ scope.row.realSalary }}</strong>
        </template>
      </el-table-column>

      <template #empty>
        <div style="padding: 20px; text-align: center; color: #999;">
          <p>暂无员工数据</p>
          <p>请先去左侧菜单 <b>【员工管理 -> 员工花名册】</b> 添加一位员工。</p>
        </div>
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.page-container { background: #fff; padding: 20px; border-radius: 8px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 20px;}
</style>