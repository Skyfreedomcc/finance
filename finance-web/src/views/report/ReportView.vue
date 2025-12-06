<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Printer, Refresh } from '@element-plus/icons-vue'

const activeTab = ref('balance')
const loading = ref(false)
// 【通用化】定义一个公司名称变量，以后可以从设置里读取
const companyName = ref('我的企业 (演示账套)')

const reportData = ref({
  // 资产
  monetaryFund: 0, receivables: 0, inventory: 0, fixedAssets: 0, totalAssets: 0,
  // 负债
  payables: 0, shortLoan: 0, totalLiabilities: 0,
  // 权益
  paidInCapital: 0, retainedEarnings: 0, totalEquity: 0,
  // 利润
  operatingIncome: 0, operatingCost: 0, operatingProfit: 0, totalProfit: 0, netProfit: 0,
  // 现金流
  cashInflow: 0, cashOutflow: 0, netCashFlow: 0
})

const loadReport = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/report/summary')
    const d = res.data || {}

    // 映射数据
    reportData.value.monetaryFund = d.assets || 0
    reportData.value.totalAssets = d.assets || 0
    reportData.value.payables = d.liabilities || 0
    reportData.value.totalLiabilities = d.liabilities || 0
    reportData.value.totalEquity = d.equity || 0
    reportData.value.paidInCapital = (d.equity || 0) - (d.profit || 0)
    reportData.value.retainedEarnings = d.profit || 0

    reportData.value.operatingIncome = d.income || 0
    reportData.value.operatingCost = d.expense || 0
    const profit = (d.income || 0) - (d.expense || 0)
    reportData.value.operatingProfit = profit
    reportData.value.totalProfit = profit
    reportData.value.netProfit = profit

    reportData.value.cashInflow = d.income || 0
    reportData.value.cashOutflow = d.expense || 0
    reportData.value.netCashFlow = d.cashNet || 0

  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const printReport = () => window.print()

onMounted(() => loadReport())
</script>

<template>
  <div class="report-container">
    <div class="toolbar no-print">
      <div class="left">
        <h2>📊 财务报表中心</h2>
      </div>
      <div class="right">
        <el-button :icon="Refresh" @click="loadReport" :loading="loading">刷新数据</el-button>
        <el-button type="primary" :icon="Printer" @click="printReport">打印报表</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="report-tabs">

      <el-tab-pane label="资产负债表" name="balance">
        <div class="paper">
          <h1 class="report-title">资 产 负 债 表</h1>
          <div class="report-meta">
            <span>编制单位：{{ companyName }}</span>
            <span>单位：元</span>
          </div>
          <table class="accounting-table">
            <thead>
            <tr class="header-row">
              <th width="30%">资 产</th><th width="20%">期末余额</th>
              <th width="30%">负债和所有者权益</th><th width="20%">期末余额</th>
            </tr>
            </thead>
            <tbody>
            <tr><td class="section-head">流动资产：</td><td></td><td class="section-head">流动负债：</td><td></td></tr>
            <tr><td>&nbsp;&nbsp;货币资金</td><td class="money">{{ reportData.monetaryFund.toFixed(2) }}</td><td>&nbsp;&nbsp;短期借款</td><td class="money">{{ reportData.shortLoan.toFixed(2) }}</td></tr>
            <tr><td>&nbsp;&nbsp;应收账款</td><td class="money">{{ reportData.receivables.toFixed(2) }}</td><td>&nbsp;&nbsp;应付账款</td><td class="money">{{ reportData.payables.toFixed(2) }}</td></tr>
            <tr><td>&nbsp;&nbsp;存货</td><td class="money">{{ reportData.inventory.toFixed(2) }}</td><td>&nbsp;&nbsp;应付职工薪酬</td><td class="money">0.00</td></tr>
            <tr><td class="section-head">非流动资产：</td><td></td><td class="section-head">所有者权益：</td><td></td></tr>
            <tr><td>&nbsp;&nbsp;固定资产</td><td class="money">{{ reportData.fixedAssets.toFixed(2) }}</td><td>&nbsp;&nbsp;实收资本</td><td class="money">{{ reportData.paidInCapital.toFixed(2) }}</td></tr>
            <tr><td></td><td></td><td>&nbsp;&nbsp;未分配利润</td><td class="money">{{ reportData.retainedEarnings.toFixed(2) }}</td></tr>
            <tr class="total-row"><td>资 产 总 计</td><td class="money">{{ reportData.totalAssets.toFixed(2) }}</td><td>负债和权益总计</td><td class="money">{{ (Number(reportData.totalLiabilities) + Number(reportData.totalEquity)).toFixed(2) }}</td></tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="利润表" name="profit">
        <div class="paper">
          <h1 class="report-title">利 润 表</h1>
          <div class="report-meta">
            <span>编制单位：{{ companyName }}</span>
            <span>单位：元</span>
          </div>
          <table class="accounting-table">
            <thead><tr class="header-row"><th width="60%">项 目</th><th width="40%">本期金额</th></tr></thead>
            <tbody>
            <tr><td class="bold">一、营业收入</td><td class="money">{{ reportData.operatingIncome.toFixed(2) }}</td></tr>
            <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;减：营业成本</td><td class="money">{{ reportData.operatingCost.toFixed(2) }}</td></tr>
            <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;税金/销售/管理费用</td><td class="money">0.00</td></tr>
            <tr class="highlight-row"><td class="bold">二、营业利润</td><td class="money bold">{{ reportData.operatingProfit.toFixed(2) }}</td></tr>
            <tr class="total-row"><td class="bold">三、净利润</td><td :class="{'money':true, 'text-red': reportData.netProfit<0}">{{ reportData.netProfit.toFixed(2) }}</td></tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="现金流量表" name="cashflow">
        <div class="paper">
          <h1 class="report-title">现 金 流 量 表</h1>
          <div class="report-meta">
            <span>编制单位：{{ companyName }}</span>
            <span>单位：元</span>
          </div>
          <table class="accounting-table">
            <thead><tr class="header-row"><th width="60%">项 目</th><th width="40%">本期金额</th></tr></thead>
            <tbody>
            <tr><td class="bold">一、经营活动产生的现金流量：</td><td></td></tr>
            <tr><td>&nbsp;&nbsp;销售商品收到的现金</td><td class="money">{{ reportData.cashInflow.toFixed(2) }}</td></tr>
            <tr><td>&nbsp;&nbsp;购买商品支付的现金</td><td class="money">{{ reportData.cashOutflow.toFixed(2) }}</td></tr>
            <tr class="highlight-row"><td class="bold">&nbsp;&nbsp;经营活动现金流量净额</td><td class="money bold">{{ reportData.netCashFlow.toFixed(2) }}</td></tr>
            <tr class="total-row"><td class="bold">四、现金净增加额</td><td class="money bold">{{ reportData.netCashFlow.toFixed(2) }}</td></tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
/* 保持您喜欢的黄色表头风格 */
.report-container { padding: 20px; background-color: #eef0f4; min-height: 100vh; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.paper { background: white; padding: 40px; border: 1px solid #dcdfe6; font-family: "SimSun", serif; color: #333; }
.report-title { text-align: center; font-size: 24px; font-weight: 900; margin-bottom: 20px; text-decoration: underline; text-underline-offset: 5px;}
.report-meta { display: flex; justify-content: space-between; margin-bottom: 5px; font-size: 13px; font-weight: bold; }
.accounting-table { width: 100%; border-collapse: collapse; border: 2px solid #333; }
.accounting-table th, .accounting-table td { border: 1px solid #888; padding: 6px 8px; font-size: 14px; }
.header-row { background-color: #ffffcc; text-align: center; font-weight: bold; }
.section-head { font-weight: bold; background-color: #f8f8f8; }
.money { text-align: right; font-family: 'Courier New', monospace; }
.bold { font-weight: bold; }
.text-red { color: red; font-weight: bold; }
.highlight-row { background-color: #f2f6fc; font-weight: bold; }
.total-row { background-color: #e1f3d8; font-weight: 900; border-top: 2px solid #333; }
@media print { .no-print, .el-tabs__header { display: none; } .report-container { padding: 0; } .paper { border: none; padding: 0; box-shadow: none; } }
</style>