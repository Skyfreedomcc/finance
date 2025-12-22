<script setup>
/**
 * 凭证序时簿（流水账）- 企业视角版
 *
 * 核心理念：从企业角度看资金变动
 * - 销售、收款、投资 = 资金流入（绿色↑）
 * - 采购、付款、工资、费用 = 资金流出（红色↓）
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const accounts = ref([])

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [txRes, accRes] = await Promise.all([
      axios.get('/financeTransaction/list'),
      axios.get('/financeAccount/list')
    ])

    list.value = txRes.data || []
    accounts.value = accRes.data || []

    // 分析每笔凭证的资金流向
    for (const tx of list.value) {
      await loadAndAnalyzeSplits(tx)
    }
  } catch (err) {
    ElMessage.error('无法加载数据')
  } finally {
    loading.value = false
  }
}

// 加载并分析分录
const loadAndAnalyzeSplits = async (tx) => {
  try {
    const res = await axios.get(`/financeSplit/list?transactionId=${tx.transactionId}`)
    tx.splits = res.data || []
    analyzeTransaction(tx)
  } catch (e) {
    tx.splits = []
    tx.effectType = 'out'
  }
}

// 分析凭证的资金流向（从企业角度）
const analyzeTransaction = (tx) => {
  const desc = (tx.description || '').toLowerCase()

  // 根据业务类型判断资金流向
  if (desc.includes('销售') || desc.includes('收款') || desc.includes('注资') || desc.includes('投资')) {
    tx.effectType = 'in'
    tx.businessType = desc.includes('销售') ? '销售收入' :
      desc.includes('注资') || desc.includes('投资') ? '股东投资' : '其他收入'
  } else if (desc.includes('采购') || desc.includes('购买') || desc.includes('入库')) {
    tx.effectType = 'out'
    tx.businessType = '采购支出'
  } else if (desc.includes('工资') || desc.includes('薪酬')) {
    tx.effectType = 'out'
    tx.businessType = '工资支出'
  } else if (desc.includes('费用')) {
    tx.effectType = 'out'
    tx.businessType = '费用支出'
  } else {
    // 默认根据分录分析
    tx.effectType = 'out'
    tx.businessType = '其他'
  }
}

// 获取业务类型标签样式
const getBusinessTag = (tx) => {
  const type = tx.businessType || '其他'
  if (type.includes('收入') || type.includes('投资')) {
    return { text: type, color: '#67c23a', bg: '#e1f3d8' }
  } else if (type.includes('采购')) {
    return { text: type, color: '#e6a23c', bg: '#fdf6ec' }
  } else if (type.includes('工资')) {
    return { text: type, color: '#f56c6c', bg: '#fde2e2' }
  } else {
    return { text: type, color: '#909399', bg: '#f4f4f5' }
  }
}

// 格式化金额
const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

// 统计
const totalIn = computed(() => {
  return list.value.filter(t => t.effectType === 'in').reduce((s, t) => s + (t.totalAmount || 0), 0)
})

const totalOut = computed(() => {
  return list.value.filter(t => t.effectType === 'out').reduce((s, t) => s + (t.totalAmount || 0), 0)
})

const netChange = computed(() => totalIn.value - totalOut.value)

onMounted(() => loadData())
</script>

<template>
  <div class="voucher-list-page">
    <div class="page-header">
      <h3>📒 凭证序时簿 (真实账本)</h3>
      <el-button type="primary" @click="loadData">刷新列表</el-button>
    </div>

    <el-table :data="list" border v-loading="loading" row-key="transactionId">
      <!-- 展开查看分录明细 -->
      <el-table-column type="expand" width="50">
        <template #default="props">
          <div class="expand-content">
            <div class="splits-title">📋 会计分录明细</div>
            <table class="splits-table">
              <thead>
              <tr>
                <th width="70">方向</th>
                <th>科目</th>
                <th>摘要</th>
                <th width="140" style="text-align: right;">金额</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="split in props.row.splits" :key="split.splitId">
                <td>
                    <span class="dir-tag" :class="split.dcDirection === 1 ? 'debit' : 'credit'">
                      {{ split.dcDirection === 1 ? '借' : '贷' }}
                    </span>
                </td>
                <td>{{ split.accountName || '未知' }}</td>
                <td>{{ split.summary || '-' }}</td>
                <td style="text-align: right; font-family: monospace;">
                  ¥ {{ formatMoney(split.amount) }}
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="transactionId" label="凭证号" width="80" align="center" />
      <el-table-column prop="voucherDate" label="日期" width="110" sortable />

      <!-- 业务类型 -->
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <span
            class="biz-tag"
            :style="{ color: getBusinessTag(row).color, background: getBusinessTag(row).bg }"
          >
            {{ getBusinessTag(row).text }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="摘要" min-width="200" />

      <!-- 资金变动 - 企业视角 -->
      <el-table-column label="资金变动" width="150" align="right">
        <template #default="{ row }">
          <div class="money-cell" :class="row.effectType">
            <span class="arrow">{{ row.effectType === 'in' ? '↑' : '↓' }}</span>
            <span class="amount">
              {{ row.effectType === 'in' ? '+' : '-' }}¥ {{ formatMoney(row.totalAmount) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'POSTED' ? 'success' : 'warning'" size="small">
            {{ row.status === 'POSTED' ? '已过账' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>

      <template #empty>
        <div class="empty-box">
          <p>📭 暂无凭证记录</p>
        </div>
      </template>
    </el-table>

    <!-- 图例 -->
    <div class="legend">
      <span class="legend-item">
        <span class="dot green"></span>
        <b>↑ 资金流入</b>：销售收款、股东投资等（企业收到钱）
      </span>
      <span class="legend-item">
        <span class="dot red"></span>
        <b>↓ 资金流出</b>：采购付款、工资发放等（企业付出钱）
      </span>
    </div>

    <!-- 统计 -->
    <div class="stats-row" v-if="list.length > 0">
      <div class="stat-card green">
        <div class="stat-label">💰 收入合计</div>
        <div class="stat-value">+¥ {{ formatMoney(totalIn) }}</div>
      </div>
      <div class="stat-card red">
        <div class="stat-label">💸 支出合计</div>
        <div class="stat-value">-¥ {{ formatMoney(totalOut) }}</div>
      </div>
      <div class="stat-card" :class="netChange >= 0 ? 'green' : 'red'">
        <div class="stat-label">📊 净变动</div>
        <div class="stat-value">{{ netChange >= 0 ? '+' : '' }}¥ {{ formatMoney(netChange) }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.voucher-list-page {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.page-header h3 { margin: 0; }

/* 业务类型标签 */
.biz-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* 资金变动单元格 */
.money-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-weight: bold;
  font-family: 'Courier New', monospace;
}

.money-cell.in { color: #67c23a; }
.money-cell.out { color: #f56c6c; }

.money-cell .arrow {
  font-size: 16px;
  margin-right: 4px;
}

/* 展开内容 */
.expand-content {
  padding: 15px 20px;
  background: #fafafa;
}

.splits-title {
  font-weight: bold;
  margin-bottom: 10px;
  color: #409eff;
}

.splits-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border: 1px solid #eee;
}

.splits-table th, .splits-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
}

.splits-table th {
  background: #f5f7fa;
  text-align: left;
}

.dir-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.dir-tag.debit { background: #e1f3d8; color: #67c23a; }
.dir-tag.credit { background: #fde2e2; color: #f56c6c; }

.empty-box {
  padding: 40px;
  text-align: center;
  color: #999;
}

/* 图例 */
.legend {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  gap: 40px;
  font-size: 13px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.green { background: #67c23a; }
.dot.red { background: #f56c6c; }

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.stat-card {
  flex: 1;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}

.stat-card.green {
  background: linear-gradient(135deg, #e1f3d8, #f0f9eb);
  border: 1px solid #c2e7b0;
}

.stat-card.red {
  background: linear-gradient(135deg, #fde2e2, #fef0f0);
  border: 1px solid #fab6b6;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  font-family: 'Courier New', monospace;
}

.stat-card.green .stat-value { color: #67c23a; }
.stat-card.red .stat-value { color: #f56c6c; }
</style>