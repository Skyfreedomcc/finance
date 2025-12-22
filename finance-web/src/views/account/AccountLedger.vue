<script setup>
/**
 * 科目明细账页面
 * 从报表页面点击科目跳转过来
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const accountId = route.params.id
const accountName = route.query.name || '未知科目'

const tableData = ref([])
const loading = ref(false)
const accountInfo = ref({})
const totalDebit = ref(0)
const totalCredit = ref(0)
const finalBalance = ref(0)

const loadLedger = async () => {
  loading.value = true
  try {
    // ✅ 使用正确的明细账接口
    const res = await axios.get(`/financeTransaction/ledger/${accountId}`)

    if (res.data) {
      accountInfo.value = res.data.account || {}
      totalDebit.value = res.data.totalDebit || 0
      totalCredit.value = res.data.totalCredit || 0
      finalBalance.value = res.data.finalBalance || 0

      // 处理明细数据
      const entries = res.data.entries || []
      tableData.value = entries.map(entry => ({
        date: entry.date || entry.voucherDate,
        no: entry.voucherId || entry.transactionId,
        desc: entry.summary || entry.description,
        debit: entry.dcDirection === 1 ? entry.amount : null,
        credit: entry.dcDirection === -1 ? entry.amount : null,
        balance: entry.balance,
        balanceDir: entry.balanceDirection
      }))
    }
  } catch (error) {
    console.error('加载明细账失败', error)
  } finally {
    loading.value = false
  }
}

// 格式化金额
const formatAmount = (val) => {
  if (val === null || val === undefined || val === 0) return '-'
  return Number(val).toFixed(2)
}

onMounted(() => loadLedger())
</script>

<template>
  <div class="ledger-box">
    <div class="header">
      <el-button icon="Back" @click="router.back()">返回</el-button>
      <h3>📂 科目明细账：{{ accountName }}</h3>
    </div>

    <!-- 汇总信息 -->
    <div class="summary-bar">
      <div class="summary-item">
        <span class="label">借方合计：</span>
        <span class="value debit">{{ formatAmount(totalDebit) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">贷方合计：</span>
        <span class="value credit">{{ formatAmount(totalCredit) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">期末余额：</span>
        <span class="value balance">{{ formatAmount(finalBalance) }}</span>
      </div>
    </div>

    <el-table :data="tableData" border stripe height="500" v-loading="loading">
      <el-table-column prop="date" label="日期" width="120" sortable />
      <el-table-column prop="no" label="凭证号" width="100" align="center" />
      <el-table-column prop="desc" label="摘要" min-width="200" />
      <el-table-column label="借方" align="right" width="150">
        <template #default="scope">
          <span v-if="scope.row.debit" style="color: #67c23a; font-weight: 500;">
            {{ formatAmount(scope.row.debit) }}
          </span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="贷方" align="right" width="150">
        <template #default="scope">
          <span v-if="scope.row.credit" style="color: #f56c6c; font-weight: 500;">
            {{ formatAmount(scope.row.credit) }}
          </span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="余额" align="right" width="150">
        <template #default="scope">
          <strong>{{ formatAmount(scope.row.balance) }}</strong>
          <small style="color: #999; margin-left: 4px;">{{ scope.row.balanceDir }}</small>
        </template>
      </el-table-column>

      <template #empty>
        <div style="padding: 40px; text-align: center; color: #999;">
          <p>该科目暂无交易记录</p>
        </div>
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.ledger-box {
  padding: 20px;
  background: white;
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}
.summary-bar {
  display: flex;
  gap: 30px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 15px;
}
.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary-item .label {
  color: #606266;
}
.summary-item .value {
  font-size: 18px;
  font-weight: bold;
}
.summary-item .value.debit {
  color: #67c23a;
}
.summary-item .value.credit {
  color: #f56c6c;
}
.summary-item .value.balance {
  color: #409eff;
}
</style>