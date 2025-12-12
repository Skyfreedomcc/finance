<script setup>
/**
 * 过账审核中心 - 增强版
 *
 * 功能：
 * 1. 显示待审核凭证列表
 * 2. 点击可展开查看分录明细
 * 3. 批量审核过账
 */
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const draftList = ref([])
const selectedIds = ref([])
const loading = ref(false)

// 加载草稿列表
const loadDrafts = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/financeTransaction/list')
    const drafts = (res.data || []).filter(t => t.status === 'DRAFT' || !t.status)

    // 为每个凭证加载分录明细
    for (const tx of drafts) {
      await loadSplits(tx)
    }

    draftList.value = drafts
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载分录明细
const loadSplits = async (tx) => {
  try {
    const res = await axios.get(`http://localhost:8080/financeSplit/list?transactionId=${tx.transactionId}`)
    tx.splits = res.data || []
  } catch (e) {
    tx.splits = []
  }
}

// 选择变化
const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.transactionId)
}

// 批量过账
const executePost = async () => {
  if (selectedIds.value.length === 0) {
    return ElMessage.warning('请至少选择一张凭证')
  }

  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 张凭证过账？过账后将更新财务报表。`,
      '过账审核',
      { type: 'warning' }
    )

    const res = await axios.post('http://localhost:8080/financeTransaction/post', selectedIds.value)
    if (res.data.code === 200) {
      ElMessage.success('过账成功！')
      loadDrafts()
    } else {
      ElMessage.error(res.data.msg || '过账失败')
    }
  } catch (e) {
    // 用户取消
  }
}

// 单个过账
const postSingle = async (tx) => {
  try {
    await ElMessageBox.confirm(
      `确定将凭证 #${tx.transactionId}「${tx.description}」过账？`,
      '确认过账',
      { type: 'warning' }
    )

    const res = await axios.post('http://localhost:8080/financeTransaction/post', [tx.transactionId])
    if (res.data.code === 200) {
      ElMessage.success('过账成功！')
      loadDrafts()
    } else {
      ElMessage.error(res.data.msg || '过账失败')
    }
  } catch (e) {
    // 用户取消
  }
}

// 删除凭证
const deleteTx = async (tx) => {
  try {
    await ElMessageBox.confirm(
      `确定删除凭证 #${tx.transactionId}「${tx.description}」？此操作不可恢复！`,
      '删除确认',
      { type: 'error' }
    )

    const res = await axios.delete(`http://localhost:8080/financeTransaction/delete/${tx.transactionId}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadDrafts()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    // 用户取消
  }
}

// 格式化金额
const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => loadDrafts())
</script>

<template>
  <div class="posting-page">
    <div class="page-header">
      <div class="header-left">
        <h3>⚖️ 过账审核中心</h3>
        <el-tag type="warning" size="small">{{ draftList.length }} 张待审核</el-tag>
      </div>
      <div class="header-right">
        <el-button @click="loadDrafts">🔄 刷新</el-button>
        <el-button type="success" :disabled="selectedIds.length === 0" @click="executePost">
          ✅ 批量过账 ({{ selectedIds.length }})
        </el-button>
      </div>
    </div>

    <el-table
      :data="draftList"
      border
      row-key="transactionId"
      @selection-change="handleSelectionChange"
      v-loading="loading"
    >
      <el-table-column type="selection" width="50" />

      <!-- 展开列 - 显示分录明细 -->
      <el-table-column type="expand" width="50">
        <template #default="props">
          <div class="expand-content">
            <div class="detail-title">📋 凭证分录明细</div>
            <table class="detail-table">
              <thead>
              <tr>
                <th width="70">方向</th>
                <th>科目</th>
                <th>摘要</th>
                <th width="130" style="text-align: right;">借方</th>
                <th width="130" style="text-align: right;">贷方</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="split in props.row.splits" :key="split.splitId">
                <td>
                    <span class="dir-badge" :class="split.dcDirection === 1 ? 'debit' : 'credit'">
                      {{ split.dcDirection === 1 ? '借' : '贷' }}
                    </span>
                </td>
                <td>{{ split.accountName || '未知科目' }}</td>
                <td>{{ split.summary || '-' }}</td>
                <td class="amount debit-col">
                  {{ split.dcDirection === 1 ? '¥ ' + formatMoney(split.amount) : '' }}
                </td>
                <td class="amount credit-col">
                  {{ split.dcDirection !== 1 ? '¥ ' + formatMoney(split.amount) : '' }}
                </td>
              </tr>
              <tr v-if="!props.row.splits || props.row.splits.length === 0">
                <td colspan="5" class="no-data">暂无分录数据</td>
              </tr>
              </tbody>
              <tfoot v-if="props.row.splits && props.row.splits.length > 0">
              <tr class="total-row">
                <td colspan="3" style="text-align: right; font-weight: bold;">合计</td>
                <td class="amount debit-col">
                  ¥ {{ formatMoney(props.row.splits.filter(s => s.dcDirection === 1).reduce((sum, s) => sum + Number(s.amount || 0), 0)) }}
                </td>
                <td class="amount credit-col">
                  ¥ {{ formatMoney(props.row.splits.filter(s => s.dcDirection !== 1).reduce((sum, s) => sum + Number(s.amount || 0), 0)) }}
                </td>
              </tr>
              </tfoot>
            </table>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="transactionId" label="凭证号" width="80" align="center" />
      <el-table-column prop="voucherDate" label="日期" width="110" sortable />
      <el-table-column prop="description" label="摘要" min-width="200" />
      <el-table-column label="金额" width="130" align="right">
        <template #default="scope">
          <span class="money">¥ {{ formatMoney(scope.row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default>
          <el-tag type="warning" size="small">草稿</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="scope">
          <el-button type="success" size="small" link @click="postSingle(scope.row)">
            过账
          </el-button>
          <el-button type="danger" size="small" link @click="deleteTx(scope.row)">
            删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <div class="empty-box">
          <p>🎉 太棒了！没有待审核的凭证</p>
          <p>所有凭证都已过账</p>
        </div>
      </template>
    </el-table>

    <!-- 提示信息 -->
    <div class="tips-box">
      <p>💡 <b>操作说明：</b></p>
      <ul>
        <li>点击凭证行左侧的 <b>▶</b> 展开查看分录明细</li>
        <li>勾选多张凭证后点击「批量过账」可一次审核多张</li>
        <li>过账后凭证将正式生效，更新财务报表</li>
        <li>草稿状态的凭证不会影响报表数据</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.posting-page {
  padding: 20px;
  background: white;
  min-height: 80vh;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h3 {
  margin: 0;
}

.header-right {
  display: flex;
  gap: 10px;
}

.money {
  font-weight: bold;
  color: #409eff;
  font-family: monospace;
}

/* 展开内容样式 */
.expand-content {
  padding: 15px 20px;
  background: #f8f9fa;
}

.detail-title {
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border: 1px solid #eee;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

.detail-table th {
  background: #f5f7fa;
  font-weight: 500;
}

.detail-table .amount {
  text-align: right;
  font-family: monospace;
  font-weight: bold;
}

.detail-table .debit-col {
  color: #67c23a;
}

.detail-table .credit-col {
  color: #f56c6c;
}

.detail-table .total-row {
  background: #fafafa;
}

.detail-table .total-row td {
  border-top: 2px solid #ddd;
}

.detail-table .no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}

.dir-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 12px;
}

.dir-badge.debit {
  background: #e1f3d8;
  color: #67c23a;
}

.dir-badge.credit {
  background: #fde2e2;
  color: #f56c6c;
}

.empty-box {
  padding: 40px;
  text-align: center;
  color: #67c23a;
}

.empty-box p {
  margin: 5px 0;
}

.tips-box {
  margin-top: 20px;
  padding: 15px;
  background: #ecf5ff;
  border-radius: 4px;
  font-size: 13px;
  color: #409eff;
}

.tips-box ul {
  margin: 10px 0 0 20px;
}

.tips-box li {
  margin: 5px 0;
}
</style>