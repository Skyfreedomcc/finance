<script setup>
/**
 * 财务报表中心
 *
 * 包含三大财务报表：
 * 1. 资产负债表
 * 2. 利润表
 * 3. 现金流量表
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const activeTab = ref('balance')
const loading = ref(false)

// 资产负债表数据
const assetTree = ref([])
const liabTree = ref([])
const totalAsset = ref(0)
const totalLiabEquity = ref(0)

// 利润表数据
const incomeData = ref({
  revenue: 0,
  cost: 0,
  grossProfit: 0,
  expense: 0,
  financeExpense: 0,
  operatingProfit: 0,
  netProfit: 0
})

// 现金流量表数据
const cashflowData = ref({
  salesCashIn: 0,
  purchaseCashOut: 0,
  salaryCashOut: 0,
  otherCashIn: 0,
  otherCashOut: 0,
  operatingCashNet: 0,
  investingCashNet: 0,
  financingCashNet: 0,
  totalCashChange: 0
})

// 加载所有数据
const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadBalanceSheet(),
      loadIncomeStatement(),
      loadCashflowStatement()
    ])
    ElMessage.success('报表数据加载完成')
  } catch (e) {
    console.error('报表加载失败', e)
    ElMessage.error('报表加载失败')
  } finally {
    loading.value = false
  }
}

// 1. 加载资产负债表
const loadBalanceSheet = async () => {
  const res = await axios.get('/report/balance-sheet')
  const data = res.data

  assetTree.value = data.asset ? [data.asset] : []

  const rightSide = []
  if (data.liability) rightSide.push(data.liability)
  if (data.equity) rightSide.push(data.equity)
  liabTree.value = rightSide

  totalAsset.value = calcTreeSum(assetTree.value)
  totalLiabEquity.value = calcTreeSum(liabTree.value)
}

// 2. 加载利润表
const loadIncomeStatement = async () => {
  try {
    const res = await axios.get('/report/income')
    incomeData.value = res.data
  } catch (e) {
    console.warn('利润表接口不可用，使用默认值')
  }
}

// 3. 加载现金流量表
const loadCashflowStatement = async () => {
  try {
    const res = await axios.get('/report/cashflow')
    cashflowData.value = res.data
  } catch (e) {
    console.warn('现金流量表接口不可用，使用默认值')
  }
}

// 递归计算树的总金额
const calcTreeSum = (nodes) => {
  let sum = 0
  for (const node of nodes) {
    if (node.children && node.children.length > 0) {
      node.amount = calcTreeSum(node.children)
    }
    sum += Number(node.amount || 0)
  }
  return sum
}

// 格式化金额
const formatMoney = (val) => {
  const num = Number(val || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 资产负债是否平衡
const isBalanced = computed(() => {
  return Math.abs(totalAsset.value - totalLiabEquity.value) < 0.01
})

onMounted(() => loadData())
</script>

<template>
  <div class="report-container" v-loading="loading">
    <div class="report-header">
      <h2>📊 财务报表中心 (Enterprise)</h2>
      <el-button type="primary" @click="loadData">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ==================== 资产负债表 ==================== -->
      <el-tab-pane label="资产负债表" name="balance">
        <div class="balance-sheet-layout">
          <!-- 左侧：资产 -->
          <div class="bs-panel">
            <div class="panel-title">资产</div>
            <el-table
              :data="assetTree"
              row-key="id"
              default-expand-all
              :tree-props="{children:'children'}"
              border
              stripe
              size="small"
            >
              <el-table-column prop="name" label="科目" min-width="200" />
              <el-table-column prop="amount" label="余额" width="150" align="right">
                <template #default="scope">
                  <span :class="{ 'negative': scope.row.amount < 0 }">
                    {{ formatMoney(scope.row.amount) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
            <div class="total-bar asset">
              资产总计: <b>¥ {{ formatMoney(totalAsset) }}</b>
            </div>
          </div>

          <!-- 右侧：负债及权益 -->
          <div class="bs-panel">
            <div class="panel-title">负债及所有者权益</div>
            <el-table
              :data="liabTree"
              row-key="id"
              default-expand-all
              :tree-props="{children:'children'}"
              border
              stripe
              size="small"
            >
              <el-table-column prop="name" label="科目" min-width="200" />
              <el-table-column prop="amount" label="余额" width="150" align="right">
                <template #default="scope">
                  <span :class="{ 'negative': scope.row.amount < 0 }">
                    {{ formatMoney(scope.row.amount) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
            <div class="total-bar liability">
              负债+权益总计: <b>¥ {{ formatMoney(totalLiabEquity) }}</b>
            </div>
          </div>
        </div>

        <!-- 平衡检查 -->
        <div class="balance-check" :class="isBalanced ? 'ok' : 'err'">
          <span v-if="isBalanced">✓ 资产负债表平衡：资产 = 负债 + 所有者权益</span>
          <span v-else>✗ 资产负债表不平衡！差额: {{ formatMoney(totalAsset - totalLiabEquity) }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 利润表 ==================== -->
      <el-tab-pane label="利润表" name="profit">
        <div class="statement-card">
          <div class="statement-title">利润表</div>
          <table class="statement-table">
            <thead>
            <tr>
              <th>项目</th>
              <th>本期金额</th>
            </tr>
            </thead>
            <tbody>
            <tr>
              <td>一、营业收入</td>
              <td class="amount">{{ formatMoney(incomeData.revenue) }}</td>
            </tr>
            <tr>
              <td class="indent">减：营业成本</td>
              <td class="amount">{{ formatMoney(incomeData.cost) }}</td>
            </tr>
            <tr class="subtotal">
              <td>二、毛利润</td>
              <td class="amount">{{ formatMoney(incomeData.grossProfit) }}</td>
            </tr>
            <tr>
              <td class="indent">减：管理费用</td>
              <td class="amount">{{ formatMoney(incomeData.expense) }}</td>
            </tr>
            <tr>
              <td class="indent">减：财务费用</td>
              <td class="amount">{{ formatMoney(incomeData.financeExpense) }}</td>
            </tr>
            <tr class="subtotal">
              <td>三、营业利润</td>
              <td class="amount">{{ formatMoney(incomeData.operatingProfit) }}</td>
            </tr>
            <tr class="total-row">
              <td>四、净利润</td>
              <td class="amount highlight">{{ formatMoney(incomeData.netProfit) }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <!-- ==================== 现金流量表 ==================== -->
      <el-tab-pane label="现金流量表" name="cash">
        <div class="statement-card">
          <div class="statement-title">现金流量表</div>
          <table class="statement-table">
            <thead>
            <tr>
              <th>项目</th>
              <th>金额</th>
            </tr>
            </thead>
            <tbody>
            <tr class="section-header">
              <td colspan="2">一、经营活动产生的现金流量</td>
            </tr>
            <tr>
              <td class="indent">销售商品、提供劳务收到的现金</td>
              <td class="amount positive">{{ formatMoney(cashflowData.salesCashIn) }}</td>
            </tr>
            <tr>
              <td class="indent">购买商品、接受劳务支付的现金</td>
              <td class="amount negative-val">-{{ formatMoney(cashflowData.purchaseCashOut) }}</td>
            </tr>
            <tr>
              <td class="indent">支付给职工的现金</td>
              <td class="amount negative-val">-{{ formatMoney(cashflowData.salaryCashOut) }}</td>
            </tr>
            <tr>
              <td class="indent">收到的其他与经营有关的现金</td>
              <td class="amount">{{ formatMoney(cashflowData.otherCashIn) }}</td>
            </tr>
            <tr>
              <td class="indent">支付的其他与经营有关的现金</td>
              <td class="amount negative-val">-{{ formatMoney(cashflowData.otherCashOut) }}</td>
            </tr>
            <tr class="subtotal">
              <td>经营活动现金流量净额</td>
              <td class="amount" :class="cashflowData.operatingCashNet >= 0 ? 'positive' : 'negative-val'">
                {{ formatMoney(cashflowData.operatingCashNet) }}
              </td>
            </tr>

            <tr class="section-header">
              <td colspan="2">二、投资活动产生的现金流量</td>
            </tr>
            <tr class="subtotal">
              <td>投资活动现金流量净额</td>
              <td class="amount">{{ formatMoney(cashflowData.investingCashNet) }}</td>
            </tr>

            <tr class="section-header">
              <td colspan="2">三、筹资活动产生的现金流量</td>
            </tr>
            <tr class="subtotal">
              <td>筹资活动现金流量净额</td>
              <td class="amount">{{ formatMoney(cashflowData.financingCashNet) }}</td>
            </tr>

            <tr class="total-row">
              <td>四、现金及现金等价物净增加额</td>
              <td class="amount highlight" :class="cashflowData.totalCashChange >= 0 ? 'positive' : 'negative-val'">
                {{ formatMoney(cashflowData.totalCashChange) }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.report-container {
  padding: 20px;
  background: #f0f2f5;
  min-height: 100vh;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.report-header h2 {
  margin: 0;
}

/* 资产负债表布局 */
.balance-sheet-layout {
  display: flex;
  gap: 20px;
}

.bs-panel {
  flex: 1;
  background: white;
  border-radius: 4px;
  overflow: hidden;
}

.panel-title {
  text-align: center;
  font-weight: bold;
  padding: 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #eee;
}

.total-bar {
  padding: 12px 15px;
  font-size: 16px;
  text-align: right;
  border-top: 2px solid;
}

.total-bar.asset {
  background: #e1f3d8;
  border-color: #67c23a;
  color: #67c23a;
}

.total-bar.liability {
  background: #fdf6ec;
  border-color: #e6a23c;
  color: #e6a23c;
}

.balance-check {
  margin-top: 20px;
  padding: 15px;
  text-align: center;
  border-radius: 4px;
  font-weight: bold;
}

.balance-check.ok {
  background: #e1f3d8;
  color: #67c23a;
}

.balance-check.err {
  background: #fde2e2;
  color: #f56c6c;
}

/* 报表卡片 */
.statement-card {
  background: white;
  border-radius: 4px;
  overflow: hidden;
}

.statement-title {
  text-align: center;
  font-weight: bold;
  font-size: 18px;
  padding: 15px;
  background: #f5f7fa;
  border-bottom: 1px solid #eee;
}

.statement-table {
  width: 100%;
  border-collapse: collapse;
}

.statement-table th,
.statement-table td {
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
}

.statement-table th {
  background: #fafafa;
  font-weight: bold;
  text-align: left;
}

.statement-table th:last-child {
  text-align: right;
  width: 200px;
}

.statement-table .indent {
  padding-left: 40px;
}

.statement-table .amount {
  text-align: right;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.statement-table .section-header {
  background: #f5f7fa;
  font-weight: bold;
}

.statement-table .subtotal {
  background: #fafafa;
  font-weight: 500;
}

.statement-table .total-row {
  background: #f0f9eb;
  font-weight: bold;
}

.statement-table .highlight {
  color: #67c23a;
  font-size: 16px;
}

.statement-table .positive {
  color: #67c23a;
}

.statement-table .negative-val {
  color: #f56c6c;
}

.negative {
  color: #f56c6c;
}
</style>