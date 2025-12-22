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
          <div class="stat-value" :class="stats.cashBalance >= 0 ? 'positive' : 'negative'">
            ¥ {{ formatMoney(stats.cashBalance) }}
          </div>
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

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 收支趋势图 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">📈 收支趋势</span>
          <span class="chart-desc">最近7天数据</span>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>

      <!-- 收支占比饼图 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">📊 本月收支构成</span>
          <span class="chart-desc">按类型分类</span>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 欢迎信息 -->
    <div class="welcome-banner">
      <div class="welcome-icon">✅</div>
      <div class="welcome-text">
        <div class="welcome-title">老板，欢迎回来！</div>
        <div class="welcome-desc">财务系统已准备就绪。数据已实时同步。</div>
      </div>
      <div class="profit-badge" v-if="stats.monthlyIncome > 0">
        <div class="profit-label">本月净利润</div>
        <div class="profit-value" :class="netProfit >= 0 ? 'positive' : 'negative'">
          {{ netProfit >= 0 ? '+' : '' }}¥ {{ formatMoney(netProfit) }}
        </div>
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
            {{ formatDate(scope.row.transactionDate || scope.row.voucherDate) }}
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
        <el-button type="warning" @click="$router.push('/payment/center')">
          💳 收付款
        </el-button>
        <el-button type="info" @click="$router.push('/report/analysis')">
          📊 查看报表
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'

const router = useRouter()
const API_BASE = ''

// 图表引用
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

// 统计数据
const stats = ref({
  monthlyIncome: 0,
  monthlyExpense: 0,
  cashBalance: 0,
  pendingCount: 0
})

// 图表数据
const trendData = ref({
  dates: [],
  incomes: [],
  expenses: []
})

const pieData = ref([])

// 最近凭证
const recentVouchers = ref([])

// 计算净利润
const netProfit = computed(() => {
  return stats.value.monthlyIncome - stats.value.monthlyExpense
})

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

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['收入', '支出'],
      top: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 50,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: trendData.value.dates,
      axisLabel: {
        rotate: 30,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (val) => {
          if (val >= 10000) return (val / 10000) + '万'
          return val
        }
      }
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: trendData.value.incomes,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67c23a' },
            { offset: 1, color: '#95d475' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '支出',
        type: 'bar',
        data: trendData.value.expenses,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f56c6c' },
            { offset: 1, color: '#fab6b6' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }

  trendChart.setOption(option)
}

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '收支构成',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: pieData.value
      }
    ]
  }

  pieChart.setOption(option)
}

// 更新图表
const updateCharts = () => {
  if (trendChart) {
    trendChart.setOption({
      xAxis: { data: trendData.value.dates },
      series: [
        { data: trendData.value.incomes },
        { data: trendData.value.expenses }
      ]
    })
  }

  if (pieChart) {
    pieChart.setOption({
      series: [{ data: pieData.value }]
    })
  }
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

    // 4. 计算图表数据
    await calculateChartData()

  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 计算图表数据
const calculateChartData = async () => {
  try {
    const [txRes, splitRes, accRes] = await Promise.all([
      axios.get(`${API_BASE}/financeTransaction/list`),
      axios.get(`${API_BASE}/financeSplit/list`),
      axios.get(`${API_BASE}/financeAccount/list`)
    ])

    if (!txRes.data || !splitRes.data || !accRes.data) return

    const transactions = txRes.data
    const splits = splitRes.data
    const accounts = accRes.data

    // 已过账的交易
    const postedTxMap = new Map()
    transactions
      .filter(tx => !tx.status || tx.status === 'POSTED')
      .forEach(tx => postedTxMap.set(tx.transactionId, tx))

    // 科目类型映射
    const accountTypeMap = new Map()
    accounts.forEach(acc => {
      accountTypeMap.set(acc.accountId, {
        type: acc.accountType,
        code: acc.accountCode || '',
        name: acc.accountName || ''
      })
    })

    // 按日期统计收支
    const dailyStats = new Map()
    const categoryStats = {
      '销售收入': 0,
      '其他收入': 0,
      '采购成本': 0,
      '工资费用': 0,
      '其他费用': 0
    }

    // 获取最近7天的日期
    const today = new Date()
    const dates = []
    for (let i = 6; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      const dateStr = date.toISOString().split('T')[0]
      dates.push(dateStr)
      dailyStats.set(dateStr, { income: 0, expense: 0 })
    }

    // 遍历分录统计
    splits.forEach(split => {
      const tx = postedTxMap.get(split.transactionId)
      if (!tx) return

      const accInfo = accountTypeMap.get(split.accountId)
      if (!accInfo) return

      const amt = Number(split.amount) || 0
      const isDebit = split.dcDirection === 1
      const voucherDate = tx.voucherDate || ''
      const desc = tx.description || ''

      // 收入类
      if (accInfo.type === 'INCOME' || accInfo.code?.startsWith('6') && !accInfo.code?.startsWith('64') && !accInfo.code?.startsWith('66')) {
        if (!isDebit) {
          // 更新每日统计
          if (dailyStats.has(voucherDate)) {
            const dayStat = dailyStats.get(voucherDate)
            dayStat.income += amt
          }

          // 更新分类统计
          if (desc.includes('销售')) {
            categoryStats['销售收入'] += amt
          } else {
            categoryStats['其他收入'] += amt
          }
        }
      }

      // 费用类
      if (accInfo.type === 'EXPENSE' || accInfo.code?.startsWith('5') || accInfo.code?.startsWith('64') || accInfo.code?.startsWith('66')) {
        if (isDebit) {
          // 更新每日统计
          if (dailyStats.has(voucherDate)) {
            const dayStat = dailyStats.get(voucherDate)
            dayStat.expense += amt
          }

          // 更新分类统计
          if (desc.includes('采购') || desc.includes('入库')) {
            categoryStats['采购成本'] += amt
          } else if (desc.includes('工资') || desc.includes('薪酬')) {
            categoryStats['工资费用'] += amt
          } else {
            categoryStats['其他费用'] += amt
          }
        }
      }
    })

    // 更新趋势图数据
    trendData.value.dates = dates.map(d => d.substring(5))  // 只显示月-日
    trendData.value.incomes = dates.map(d => dailyStats.get(d)?.income || 0)
    trendData.value.expenses = dates.map(d => dailyStats.get(d)?.expense || 0)

    // 更新饼图数据
    pieData.value = [
      { value: categoryStats['销售收入'], name: '销售收入', itemStyle: { color: '#67c23a' } },
      { value: categoryStats['其他收入'], name: '其他收入', itemStyle: { color: '#95d475' } },
      { value: categoryStats['采购成本'], name: '采购成本', itemStyle: { color: '#f56c6c' } },
      { value: categoryStats['工资费用'], name: '工资费用', itemStyle: { color: '#e6a23c' } },
      { value: categoryStats['其他费用'], name: '其他费用', itemStyle: { color: '#909399' } }
    ].filter(item => item.value > 0)

    // 如果没有数据，显示默认
    if (pieData.value.length === 0) {
      pieData.value = [
        { value: stats.value.monthlyIncome || 1, name: '收入', itemStyle: { color: '#67c23a' } },
        { value: stats.value.monthlyExpense || 1, name: '支出', itemStyle: { color: '#f56c6c' } }
      ]
    }

    updateCharts()

  } catch (e) {
    console.error('计算图表数据失败:', e)
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

    const postedTxIds = new Set(
      txRes.data
        .filter(tx => !tx.status || tx.status === 'POSTED')
        .map(tx => tx.transactionId)
    )

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

      if (accInfo.type === 'INCOME' || accInfo.code.startsWith('6') || accInfo.name.includes('收入')) {
        if (isCredit) {
          totalIncome += amt
        }
      }
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

    const postedTxIds = new Set(
      txRes.data
        .filter(tx => !tx.status || tx.status === 'POSTED')
        .map(tx => tx.transactionId)
    )

    const cashAccountIds = new Set()
    for (const acc of accountRes.data) {
      const code = acc.accountCode || ''
      const name = acc.accountName || ''
      if (code === '1001' || code === '1002' ||
        name.includes('现金') || name.includes('银行存款') || name.includes('货币资金')) {
        cashAccountIds.add(acc.accountId)
      }
    }

    let totalCash = 0
    for (const split of splitRes.data) {
      if (!postedTxIds.has(split.transactionId)) continue
      if (!cashAccountIds.has(split.accountId)) continue

      const amt = Number(split.amount) || 0
      if (split.dcDirection === 1) {
        totalCash += amt
      } else {
        totalCash -= amt
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

    const sorted = txRes.data
      .sort((a, b) => {
        const dateA = new Date(a.voucherDate || a.transactionDate || 0)
        const dateB = new Date(b.voucherDate || b.transactionDate || 0)
        return dateB - dateA
      })
      .slice(0, 6)

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

// 窗口大小改变时重绘图表
const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

// 页面加载时获取数据
onMounted(async () => {
  await loadStats()
  await loadRecentVouchers()

  // 等待DOM渲染后初始化图表
  await nextTick()
  initTrendChart()
  initPieChart()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
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
.stat-card.expense .stat-value { color: #f56c6c; }
.stat-card.cash .stat-value.positive { color: #67c23a; }
.stat-card.cash .stat-value.negative { color: #f56c6c; }
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

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chart-desc {
  font-size: 12px;
  color: #999;
}

.chart-container {
  height: 280px;
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

.welcome-text {
  flex: 1;
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

.profit-badge {
  text-align: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
  border-radius: 8px;
  border: 1px solid #c2e7b0;
}

.profit-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.profit-value {
  font-size: 22px;
  font-weight: 700;
}

.profit-value.positive { color: #67c23a; }
.profit-value.negative { color: #f56c6c; }

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

  .charts-row {
    grid-template-columns: 1fr;
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

  .welcome-banner {
    flex-direction: column;
    text-align: center;
  }

  .profit-badge {
    width: 100%;
  }
}
</style>