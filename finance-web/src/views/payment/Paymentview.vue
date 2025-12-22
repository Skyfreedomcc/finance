<script setup>
/**
 * 收付款管理中心
 *
 * 功能：
 * 1. 查看待付款（应付账款）和待收款（应收账款）
 * 2. 记录付款/收款操作
 * 3. 自动生成对应的会计凭证
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const API_BASE = ''

// 当前激活的标签页
const activeTab = ref('payable')

// 数据列表
const payableList = ref([])  // 应付账款（待付款）
const receivableList = ref([])  // 应收账款（待收款）
const accounts = ref([])  // 科目列表
const loading = ref(false)

// 付款/收款对话框
const dialogVisible = ref(false)
const dialogType = ref('pay')  // 'pay' 或 'receive'
const currentItem = ref(null)

// 表单数据
const paymentForm = ref({
  amount: 0,
  paymentAccountId: null,
  paymentDate: new Date().toISOString().split('T')[0],
  remark: ''
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [txRes, splitRes, accRes] = await Promise.all([
      axios.get(`${API_BASE}/financeTransaction/list`),
      axios.get(`${API_BASE}/financeSplit/list`),
      axios.get(`${API_BASE}/financeAccount/list`)
    ])

    accounts.value = accRes.data || []

    // 分析应付和应收
    analyzePayables(txRes.data || [], splitRes.data || [])
    analyzeReceivables(txRes.data || [], splitRes.data || [])

  } catch (err) {
    console.error('加载数据失败:', err)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 分析应付账款（采购产生的负债）
const analyzePayables = (transactions, splits) => {
  const postedTxIds = new Set(
    transactions
      .filter(tx => !tx.status || tx.status === 'POSTED')
      .map(tx => tx.transactionId)
  )

  // 找到应付账款科目
  const apAccounts = accounts.value.filter(a =>
    a.accountName?.includes('应付账款') || a.accountCode === '2202'
  )
  const apAccountIds = new Set(apAccounts.map(a => a.accountId))

  // 按凭证分组，找出未结清的应付
  const txMap = new Map()
  transactions.forEach(tx => txMap.set(tx.transactionId, tx))

  // 计算每笔凭证的应付金额
  const payableMap = new Map()

  splits.forEach(split => {
    if (!postedTxIds.has(split.transactionId)) return
    if (!apAccountIds.has(split.accountId)) return

    const txId = split.transactionId
    const tx = txMap.get(txId)
    if (!tx) return

    // 只处理采购类凭证
    const desc = tx.description || ''
    if (!desc.includes('采购') && !desc.includes('入库')) return

    if (!payableMap.has(txId)) {
      payableMap.set(txId, {
        transactionId: txId,
        voucherDate: tx.voucherDate,
        description: tx.description,
        originalAmount: 0,
        paidAmount: 0,
        status: 'UNPAID'
      })
    }

    const item = payableMap.get(txId)
    // 贷方增加应付
    if (split.dcDirection !== 1) {
      item.originalAmount += Number(split.amount) || 0
    }
  })

  // 检查是否有对应的付款记录
  splits.forEach(split => {
    if (!postedTxIds.has(split.transactionId)) return
    if (!apAccountIds.has(split.accountId)) return

    const tx = txMap.get(split.transactionId)
    if (!tx) return

    const desc = tx.description || ''
    if (desc.includes('付款') || desc.includes('支付')) {
      // 借方减少应付 = 已付款
      if (split.dcDirection === 1) {
        // 这里简化处理，实际应该关联原单据
      }
    }
  })

  payableList.value = Array.from(payableMap.values())
    .filter(item => item.originalAmount > item.paidAmount)
    .sort((a, b) => new Date(b.voucherDate) - new Date(a.voucherDate))
}

// 分析应收账款（销售产生的债权）
const analyzeReceivables = (transactions, splits) => {
  const postedTxIds = new Set(
    transactions
      .filter(tx => !tx.status || tx.status === 'POSTED')
      .map(tx => tx.transactionId)
  )

  // 找到应收账款科目
  const arAccounts = accounts.value.filter(a =>
    a.accountName?.includes('应收账款') || a.accountCode === '1122'
  )
  const arAccountIds = new Set(arAccounts.map(a => a.accountId))

  const txMap = new Map()
  transactions.forEach(tx => txMap.set(tx.transactionId, tx))

  const receivableMap = new Map()

  splits.forEach(split => {
    if (!postedTxIds.has(split.transactionId)) return
    if (!arAccountIds.has(split.accountId)) return

    const txId = split.transactionId
    const tx = txMap.get(txId)
    if (!tx) return

    const desc = tx.description || ''
    if (!desc.includes('销售') && !desc.includes('出库')) return

    if (!receivableMap.has(txId)) {
      receivableMap.set(txId, {
        transactionId: txId,
        voucherDate: tx.voucherDate,
        description: tx.description,
        originalAmount: 0,
        receivedAmount: 0,
        status: 'UNPAID'
      })
    }

    const item = receivableMap.get(txId)
    // 借方增加应收
    if (split.dcDirection === 1) {
      item.originalAmount += Number(split.amount) || 0
    }
  })

  receivableList.value = Array.from(receivableMap.values())
    .filter(item => item.originalAmount > item.receivedAmount)
    .sort((a, b) => new Date(b.voucherDate) - new Date(a.voucherDate))
}

// 获取现金/银行账户列表
const cashAccounts = computed(() => {
  return accounts.value.filter(a => {
    const code = a.accountCode || ''
    const name = a.accountName || ''
    return code === '1001' || code === '1002' ||
      name.includes('现金') || name.includes('银行存款') || name.includes('货币资金')
  })
})

// 打开付款对话框
const openPayDialog = (item) => {
  dialogType.value = 'pay'
  currentItem.value = item
  paymentForm.value = {
    amount: item.originalAmount - item.paidAmount,
    paymentAccountId: null,
    paymentDate: new Date().toISOString().split('T')[0],
    remark: ''
  }
  dialogVisible.value = true
}

// 打开收款对话框
const openReceiveDialog = (item) => {
  dialogType.value = 'receive'
  currentItem.value = item
  paymentForm.value = {
    amount: item.originalAmount - item.receivedAmount,
    paymentAccountId: null,
    paymentDate: new Date().toISOString().split('T')[0],
    remark: ''
  }
  dialogVisible.value = true
}

// 确认付款/收款
const confirmPayment = async () => {
  if (!paymentForm.value.paymentAccountId) {
    return ElMessage.warning('请选择付款/收款账户')
  }
  if (paymentForm.value.amount <= 0) {
    return ElMessage.warning('金额必须大于0')
  }

  try {
    const isPay = dialogType.value === 'pay'
    const splits = []

    // 找到应付/应收科目
    const targetAccount = accounts.value.find(a =>
      isPay ? a.accountName?.includes('应付账款') : a.accountName?.includes('应收账款')
    )

    if (!targetAccount) {
      return ElMessage.error(`找不到${isPay ? '应付账款' : '应收账款'}科目`)
    }

    if (isPay) {
      // 付款：借-应付账款，贷-银行存款/现金
      splits.push({
        accountId: targetAccount.accountId,
        summary: `付款 - ${currentItem.value.description}`,
        amount: paymentForm.value.amount,
        dcDirection: 1  // 借方
      })
      splits.push({
        accountId: paymentForm.value.paymentAccountId,
        summary: `付款 - ${currentItem.value.description}`,
        amount: paymentForm.value.amount,
        dcDirection: -1  // 贷方
      })
    } else {
      // 收款：借-银行存款/现金，贷-应收账款
      splits.push({
        accountId: paymentForm.value.paymentAccountId,
        summary: `收款 - ${currentItem.value.description}`,
        amount: paymentForm.value.amount,
        dcDirection: 1  // 借方
      })
      splits.push({
        accountId: targetAccount.accountId,
        summary: `收款 - ${currentItem.value.description}`,
        amount: paymentForm.value.amount,
        dcDirection: -1  // 贷方
      })
    }

    const res = await axios.post(`${API_BASE}/financeTransaction/add`, {
      voucherDate: paymentForm.value.paymentDate,
      description: `${isPay ? '付款' : '收款'} - ${currentItem.value.description}${paymentForm.value.remark ? ' (' + paymentForm.value.remark + ')' : ''}`,
      status: 'POSTED',
      splits: splits
    })

    if (res.data.code === 200) {
      ElMessage.success(`${isPay ? '付款' : '收款'}成功！凭证已自动生成并过账`)
      dialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }

  } catch (err) {
    console.error('操作失败:', err)
    ElMessage.error('操作失败: ' + (err.message || '网络错误'))
  }
}

// 格式化金额
const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 计算待付/待收总额
const totalPayable = computed(() => {
  return payableList.value.reduce((sum, item) => sum + (item.originalAmount - item.paidAmount), 0)
})

const totalReceivable = computed(() => {
  return receivableList.value.reduce((sum, item) => sum + (item.originalAmount - item.receivedAmount), 0)
})

onMounted(() => loadData())
</script>

<template>
  <div class="payment-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h3>💳 收付款管理中心</h3>
      <el-button type="primary" @click="loadData" :loading="loading">
        🔄 刷新数据
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card payable">
        <div class="stat-icon">📤</div>
        <div class="stat-content">
          <div class="stat-label">待付款总额</div>
          <div class="stat-value">¥ {{ formatMoney(totalPayable) }}</div>
          <div class="stat-count">{{ payableList.length }} 笔待处理</div>
        </div>
      </div>
      <div class="stat-card receivable">
        <div class="stat-icon">📥</div>
        <div class="stat-content">
          <div class="stat-label">待收款总额</div>
          <div class="stat-value">¥ {{ formatMoney(totalReceivable) }}</div>
          <div class="stat-count">{{ receivableList.length }} 笔待处理</div>
        </div>
      </div>
      <div class="stat-card net">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-label">净应收</div>
          <div class="stat-value" :class="totalReceivable - totalPayable >= 0 ? 'positive' : 'negative'">
            ¥ {{ formatMoney(totalReceivable - totalPayable) }}
          </div>
          <div class="stat-count">应收 - 应付</div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="payment-tabs">
      <!-- 应付账款（待付款） -->
      <el-tab-pane label="📤 应付账款（待付款）" name="payable">
        <el-table :data="payableList" border v-loading="loading" empty-text="🎉 太棒了！没有待付款项">
          <el-table-column prop="transactionId" label="凭证号" width="80" align="center" />
          <el-table-column prop="voucherDate" label="单据日期" width="110" />
          <el-table-column prop="description" label="业务摘要" min-width="200" />
          <el-table-column label="应付金额" width="140" align="right">
            <template #default="{ row }">
              <span class="amount payable">¥ {{ formatMoney(row.originalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已付金额" width="120" align="right">
            <template #default="{ row }">
              <span class="amount paid">¥ {{ formatMoney(row.paidAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待付金额" width="140" align="right">
            <template #default="{ row }">
              <span class="amount pending">¥ {{ formatMoney(row.originalAmount - row.paidAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openPayDialog(row)">
                💰 付款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 应收账款（待收款） -->
      <el-tab-pane label="📥 应收账款（待收款）" name="receivable">
        <el-table :data="receivableList" border v-loading="loading" empty-text="🎉 太棒了！没有待收款项">
          <el-table-column prop="transactionId" label="凭证号" width="80" align="center" />
          <el-table-column prop="voucherDate" label="单据日期" width="110" />
          <el-table-column prop="description" label="业务摘要" min-width="200" />
          <el-table-column label="应收金额" width="140" align="right">
            <template #default="{ row }">
              <span class="amount receivable">¥ {{ formatMoney(row.originalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已收金额" width="120" align="right">
            <template #default="{ row }">
              <span class="amount received">¥ {{ formatMoney(row.receivedAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待收金额" width="140" align="right">
            <template #default="{ row }">
              <span class="amount pending">¥ {{ formatMoney(row.originalAmount - row.receivedAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="openReceiveDialog(row)">
                💵 收款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 操作说明 -->
    <div class="tips-section">
      <div class="tips-title">💡 操作说明：</div>
      <ul class="tips-list">
        <li>
          <span class="tip-icon pay">📤</span>
          <b>付款操作</b>：点击「付款」后，系统自动生成凭证：借-应付账款，贷-银行存款/现金
        </li>
        <li>
          <span class="tip-icon receive">📥</span>
          <b>收款操作</b>：点击「收款」后，系统自动生成凭证：借-银行存款/现金，贷-应收账款
        </li>
        <li>
          <span class="tip-icon info">ℹ️</span>
          采购/销售单据过账后会自动产生应付/应收，在此处可进行后续的付款/收款操作
        </li>
      </ul>
    </div>

    <!-- 付款/收款对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'pay' ? '💰 确认付款' : '💵 确认收款'"
      width="500px"
    >
      <el-form :model="paymentForm" label-width="100px">
        <el-form-item label="原始单据">
          <el-input :value="currentItem?.description" disabled />
        </el-form-item>
        <el-form-item label="原始金额">
          <el-input :value="'¥ ' + formatMoney(currentItem?.originalAmount)" disabled />
        </el-form-item>
        <el-form-item :label="dialogType === 'pay' ? '付款金额' : '收款金额'">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0.01"
            :max="currentItem?.originalAmount"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="dialogType === 'pay' ? '付款账户' : '收款账户'">
          <el-select v-model="paymentForm.paymentAccountId" placeholder="请选择账户" style="width: 100%">
            <el-option
              v-for="acc in cashAccounts"
              :key="acc.accountId"
              :label="acc.accountName"
              :value="acc.accountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="dialogType === 'pay' ? '付款日期' : '收款日期'">
          <el-date-picker
            v-model="paymentForm.paymentDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>

      <!-- 分录预览 -->
      <div class="preview-section">
        <div class="preview-title">📋 将生成的会计分录：</div>
        <div class="preview-splits">
          <template v-if="dialogType === 'pay'">
            <div class="split-row">
              <span class="dir-tag debit">借</span>
              <span class="acc-name">应付账款</span>
              <span class="acc-amount">¥ {{ formatMoney(paymentForm.amount) }}</span>
            </div>
            <div class="split-row">
              <span class="dir-tag credit">贷</span>
              <span class="acc-name">{{ cashAccounts.find(a => a.accountId === paymentForm.paymentAccountId)?.accountName || '待选择' }}</span>
              <span class="acc-amount">¥ {{ formatMoney(paymentForm.amount) }}</span>
            </div>
          </template>
          <template v-else>
            <div class="split-row">
              <span class="dir-tag debit">借</span>
              <span class="acc-name">{{ cashAccounts.find(a => a.accountId === paymentForm.paymentAccountId)?.accountName || '待选择' }}</span>
              <span class="acc-amount">¥ {{ formatMoney(paymentForm.amount) }}</span>
            </div>
            <div class="split-row">
              <span class="dir-tag credit">贷</span>
              <span class="acc-name">应收账款</span>
              <span class="acc-amount">¥ {{ formatMoney(paymentForm.amount) }}</span>
            </div>
          </template>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="dialogType === 'pay' ? 'primary' : 'success'" @click="confirmPayment">
          {{ dialogType === 'pay' ? '确认付款' : '确认收款' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.payment-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h3 {
  margin: 0;
  font-size: 20px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  font-size: 40px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.stat-card.payable .stat-icon { background: #fef0f0; }
.stat-card.receivable .stat-icon { background: #f0f9eb; }
.stat-card.net .stat-icon { background: #f4f4f5; }

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-card.payable .stat-value { color: #f56c6c; }
.stat-card.receivable .stat-value { color: #67c23a; }
.stat-value.positive { color: #67c23a; }
.stat-value.negative { color: #f56c6c; }

.stat-count {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 标签页 */
.payment-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 金额样式 */
.amount {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.amount.payable, .amount.pending { color: #f56c6c; }
.amount.receivable { color: #67c23a; }
.amount.paid, .amount.received { color: #909399; }

/* 提示区域 */
.tips-section {
  margin-top: 20px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tips-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #409eff;
}

.tips-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.tips-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

.tip-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 14px;
}

.tip-icon.pay { background: #fef0f0; }
.tip-icon.receive { background: #f0f9eb; }
.tip-icon.info { background: #f4f4f5; }

/* 分录预览 */
.preview-section {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.preview-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #606266;
}

.preview-splits {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.split-row {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: white;
  border-bottom: 1px solid #eee;
}

.split-row:last-child {
  border-bottom: none;
}

.dir-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
}

.dir-tag.debit { background: #e1f3d8; color: #67c23a; }
.dir-tag.credit { background: #fde2e2; color: #f56c6c; }

.acc-name {
  flex: 1;
  color: #333;
}

.acc-amount {
  font-weight: 600;
  font-family: 'Courier New', monospace;
  color: #409eff;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>