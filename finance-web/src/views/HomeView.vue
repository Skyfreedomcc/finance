<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <div class="stats-row">
      <div class="stat-card income">
        <div class="stat-content">
          <div class="stat-label">本月销售额 (收入)</div>
          <div class="stat-value">¥ {{ formatMoney(stats.monthlyIncome) }}</div>
          <div class="stat-desc">实时财务数据</div>
        </div>
        <div class="stat-icon">💰</div>
      </div>

      <div class="stat-card expense">
        <div class="stat-content">
          <div class="stat-label">本月支出 (成本)</div>
          <div class="stat-value">¥ {{ formatMoney(stats.monthlyExpense) }}</div>
          <div class="stat-desc">房租/进货/工资</div>
        </div>
        <div class="stat-icon">💸</div>
      </div>

      <div class="stat-card cash">
        <div class="stat-content">
          <div class="stat-label">现金及银行余额</div>
          <div class="stat-value">¥ {{ formatMoney(stats.cashBalance) }}</div>
          <div class="stat-desc">资产类现金科目汇总</div>
        </div>
        <div class="stat-icon">🏦</div>
      </div>

      <div class="stat-card pending">
        <div class="stat-content">
          <div class="stat-label">待处理单据</div>
          <div class="stat-value highlight">{{ stats.pendingCount }} 笔</div>
          <div class="stat-desc">待支付与待收款项</div>
        </div>
        <div class="stat-icon">📋</div>
      </div>
    </div>

    <!-- 欢迎信息 -->
    <div class="welcome-banner">
      <div class="welcome-icon">✅</div>
      <div class="welcome-text">
        <div class="welcome-title">老板，欢迎回来！</div>
        <div class="welcome-desc">财务系统已准备就绪。数据已实时同步。</div>
      </div>
    </div>

    <!-- 最近凭证 -->
    <div class="recent-section">
      <div class="section-header">
        <span class="section-title">📄 最近录入的凭证</span>
        <span class="view-all" @click="goToVoucherList">查看全部 →</span>
      </div>

      <el-table :data="recentVouchers" style="width: 100%" v-if="recentVouchers.length > 0">
        <el-table-column prop="transactionId" label="凭证号" width="100" />
        <el-table-column prop="transactionDate" label="日期" width="120">
          <template #default="scope">
            {{ formatDate(scope.row.transactionDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="摘要" />
        <el-table-column prop="totalAmount" label="金额" width="150" align="right">
          <template #default="scope">
            <span class="amount">¥ {{ formatMoney(scope.row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'POSTED' ? 'success' : 'warning'" size="small">
              {{ scope.row.status === 'POSTED' ? '已过账' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="empty-hint">
        <div class="empty-icon">📝</div>
        <div class="empty-text">暂无凭证记录</div>
        <div class="empty-desc">请先在「凭证录入」或「采购/销售」模块录入业务数据</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <div class="section-title">🚀 快捷操作</div>
      <div class="action-buttons">
        <el-button type="primary" @click="$router.push('/voucher/entry')">
          📝 录入凭证
        </el-button>
        <el-button type="danger" @click="$router.push('/invoice/sale')">
          🛒 新建销售
        </el-button>
        <el-button type="success" @click="$router.push('/invoice/purchase')">
          🛍️ 新建采购
        </el-button>
        <el-button type="info" @click="$router.push('/report/analysis')">
          📊 查看报表
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const API_BASE = 'http://localhost:8080'

// 统计数据
const stats = ref({
  monthlyIncome: 0,
  monthlyExpense: 0,
  cashBalance: 0,
  pendingCount: 0
})

// 最近凭证
const recentVouchers = ref([])

// 格式化金额
const formatMoney = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

// 跳转到凭证列表
const goToVoucherList = () => {
  router.push('/voucher/list')
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 1. 从利润表获取收入和支出
    try {
      const incomeRes = await axios.get(`${API_BASE}/report/income`)
      if (incomeRes.data) {
        stats.value.monthlyIncome = Number(incomeRes.data.revenue) || 0
        stats.value.monthlyExpense = (Number(incomeRes.data.cost) || 0) + (Number(incomeRes.data.expense) || 0)
      }
    } catch (e) {
      console.log('利润表接口调用失败，尝试从分录计算')
      await calculateIncomeExpenseFromSplits()
    }

    // 2. 计算现金余额
    await calculateCashBalance()

    // 3. 获取待处理单据数量
    try {
      const txRes = await axios.get(`${API_BASE}/financeTransaction/list`)
      if (txRes.data && Array.isArray(txRes.data)) {
        stats.value.pendingCount = txRes.data.filter(tx => tx.status === 'DRAFT').length
      }
    } catch (e) {
      console.log('获取交易列表失败')
    }

  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 从分录计算收入和支出
const calculateIncomeExpenseFromSplits = async () => {
  try {
    const [accountRes, splitRes, txRes] = await Promise.all([
      axios.get(`${API_BASE}/financeAccount/list`),
      axios.get(`${API_BASE}/financeSplit/list`),
      axios.get(`${API_BASE}/financeTransaction/list`)
    ])

    if (!accountRes.data || !splitRes.data || !txRes.data) return

    // 已过账的交易ID
    const postedTxIds = new Set(
      txRes.data
        .filter(tx => !tx.status || tx.status === 'POSTED')
        .map(tx => tx.transactionId)
    )

    // 科目类型映射
    const accountTypeMap = new Map()
    for (const acc of accountRes.data) {
      accountTypeMap.set(acc.accountId, {
        type: acc.accountType,
        code: acc.accountCode || '',
        name: acc.accountName || ''
      })
    }

    let totalIncome = 0
    let totalExpense = 0

    for (const split of splitRes.data) {
      if (!postedTxIds.has(split.transactionId)) continue

      const accInfo = accountTypeMap.get(split.accountId)
      if (!accInfo) continue

      const amt = Number(split.amount) || 0
      const isCredit = split.dcDirection !== 1

      // 收入类科目
      if (accInfo.type === 'INCOME' || accInfo.code.startsWith('6') || accInfo.name.includes('收入')) {
        if (isCredit) {
          totalIncome += amt
        }
      }
      // 费用类科目
      else if (accInfo.type === 'EXPENSE' || accInfo.code.startsWith('5') || accInfo.code.startsWith('64') || accInfo.name.includes('费用') || accInfo.name.includes('成本')) {
        if (!isCredit) {
          totalExpense += amt
        }
      }
    }

    stats.value.monthlyIncome = totalIncome
    stats.value.monthlyExpense = totalExpense

  } catch (e) {
    console.error('从分录计算收入支出失败:', e)
  }
}

// 计算现金余额
const calculateCashBalance = async () => {
  try {
    const [accountRes, splitRes, txRes] = await Promise.all([
      axios.get(`${API_BASE}/financeAccount/list`),
      axios.get(`${API_BASE}/financeSplit/list`),
      axios.get(`${API_BASE}/financeTransaction/list`)
    ])

    if (!accountRes.data || !splitRes.data || !txRes.data) return

    // 已过账的交易ID
    const postedTxIds = new Set(
      txRes.data
        .filter(tx => !tx.status || tx.status === 'POSTED')
        .map(tx => tx.transactionId)
    )

    // 找到现金类科目
    const cashAccountIds = new Set()
    for (const acc of accountRes.data) {
      const code = acc.accountCode || ''
      const name = acc.accountName || ''
      if (code === '1001' || code === '1002' ||
        name.includes('现金') || name.includes('银行存款') || name.includes('货币资金')) {
        cashAccountIds.add(acc.accountId)
      }
    }

    // 计算现金余额
    let totalCash = 0
    for (const split of splitRes.data) {
      if (!postedTxIds.has(split.transactionId)) continue
      if (!cashAccountIds.has(split.accountId)) continue

      const amt = Number(split.amount) || 0
      if (split.dcDirection === 1) {
        totalCash += amt  // 借方增加
      } else {
        totalCash -= amt  // 贷方减少
      }
    }

    stats.value.cashBalance = totalCash

  } catch (e) {
    console.error('计算现金余额失败:', e)
  }
}

// 加载最近凭证
const loadRecentVouchers = async () => {
  try {
    const [txRes, splitRes] = await Promise.all([
      axios.get(`${API_BASE}/financeTransaction/list`),
      axios.get(`${API_BASE}/financeSplit/list`)
    ])

    if (!txRes.data || !Array.isArray(txRes.data)) return

    // 按日期排序，取最近10条
    const sorted = txRes.data
      .sort((a, b) => {
        const dateA = new Date(a.transactionDate || 0)
        const dateB = new Date(b.transactionDate || 0)
        return dateB - dateA
      })
      .slice(0, 10)

    // 计算每条凭证的总金额
    const splitsMap = new Map()
    if (splitRes.data && Array.isArray(splitRes.data)) {
      for (const split of splitRes.data) {
        const txId = split.transactionId
        if (!splitsMap.has(txId)) {
          splitsMap.set(txId, 0)
        }
        if (split.dcDirection === 1) {
          splitsMap.set(txId, splitsMap.get(txId) + (Number(split.amount) || 0))
        }
      }
    }

    recentVouchers.value = sorted.map(tx => ({
      ...tx,
      totalAmount: splitsMap.get(tx.transactionId) || 0
    }))

  } catch (error) {
    console.error('加载凭证列表失败:', error)
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadStats()
  loadRecentVouchers()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-card.income .stat-value { color: #333; }
.stat-card.expense .stat-value { color: #e74c3c; }
.stat-card.cash .stat-value { color: #e74c3c; }
.stat-card.pending .stat-value { color: #3498db; }

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.8;
}

/* 欢迎横幅 */
.welcome-banner {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  border-left: 4px solid #27ae60;
}

.welcome-icon {
  font-size: 32px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #27ae60;
}

.welcome-desc {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

/* 最近凭证区域 */
.recent-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.view-all {
  font-size: 14px;
  color: #3498db;
  cursor: pointer;
}

.view-all:hover {
  text-decoration: underline;
}

.amount {
  font-weight: 600;
  color: #e74c3c;
}

/* 空状态 */
.empty-hint {
  text-align: center;
  padding: 40px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
}

/* 快捷操作 */
.quick-actions {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.quick-actions .section-title {
  margin-bottom: 16px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  padding: 12px 24px;
  font-size: 15px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }
}
</style>