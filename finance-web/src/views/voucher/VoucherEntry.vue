<script setup>
/**
 * 凭证录入 - 增强版
 *
 * 功能：
 * 1. 支持多借多贷
 * 2. 每行显示：数量、单价、金额
 * 3. 只能保存草稿，统一在过账中心审核
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const accountOptions = ref([])
const loading = ref(false)

// 凭证主数据
const form = ref({
  voucherDate: new Date().toISOString().split('T')[0],
  description: ''
})

// 分录列表 - 增加数量和单价
const splits = ref([
  { accountId: null, summary: '', dcDirection: 1, quantity: 1, price: 0, amount: 0 },
  { accountId: null, summary: '', dcDirection: -1, quantity: 1, price: 0, amount: 0 }
])

// 加载科目树
const loadAccountTree = async () => {
  try {
    const res = await axios.get('http://localhost:8080/financeAccount/list')
    accountOptions.value = handleTree(res.data || [], "accountId", "parentId")
  } catch (e) {
    ElMessage.error('科目加载失败')
  }
}

// 列表转树
function handleTree(data, id, parentId, children) {
  let config = { id: id || 'id', parentId: parentId || 'parentId', childrenList: children || 'children' }
  var childrenListMap = {}
  var nodeIds = {}
  var tree = []

  for (let d of data) {
    let pId = d[config.parentId]
    if (childrenListMap[pId] == null) childrenListMap[pId] = []
    nodeIds[d[config.id]] = d
    childrenListMap[pId].push(d)
  }

  for (let d of data) {
    let pId = d[config.parentId]
    if (nodeIds[pId] == null) tree.push(d)
  }

  for (let t of tree) adaptToChildrenList(t)

  function adaptToChildrenList(o) {
    if (childrenListMap[o[config.id]] !== null) {
      o[config.childrenList] = childrenListMap[o[config.id]]
    }
    if (o[config.childrenList]) {
      for (let c of o[config.childrenList]) adaptToChildrenList(c)
    }
  }
  return tree
}

// 计算行金额
const calcRowAmount = (row) => {
  row.amount = Number(row.quantity || 1) * Number(row.price || 0)
}

// 计算借方合计
const totalDebit = computed(() => {
  return splits.value
    .filter(r => r.dcDirection === 1)
    .reduce((s, r) => s + Number(r.amount || 0), 0)
})

// 计算贷方合计
const totalCredit = computed(() => {
  return splits.value
    .filter(r => r.dcDirection === -1)
    .reduce((s, r) => s + Number(r.amount || 0), 0)
})

// 是否平衡
const isBalanced = computed(() => {
  return Math.abs(totalDebit.value - totalCredit.value) < 0.01
})

// 差额
const difference = computed(() => {
  return Math.abs(totalDebit.value - totalCredit.value)
})

// 添加借方行
const addDebitRow = () => {
  splits.value.push({
    accountId: null,
    summary: form.value.description,
    dcDirection: 1,
    quantity: 1,
    price: 0,
    amount: 0
  })
}

// 添加贷方行
const addCreditRow = () => {
  splits.value.push({
    accountId: null,
    summary: form.value.description,
    dcDirection: -1,
    quantity: 1,
    price: 0,
    amount: 0
  })
}

// 删除行
const removeRow = (index) => {
  if (splits.value.length <= 2) {
    ElMessage.warning('至少保留两行分录')
    return
  }
  splits.value.splice(index, 1)
}

// 切换借贷方向
const toggleDirection = (row) => {
  row.dcDirection = row.dcDirection === 1 ? -1 : 1
}

// 自动填充差额
const autoFillBalance = (row) => {
  if (row.amount > 0 || row.price > 0) return

  const diff = difference.value
  if (diff > 0) {
    row.price = diff
    row.quantity = 1
    row.amount = diff
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  await saveVoucher('DRAFT')
}

// 保存并过账
const handleSaveAndPost = async () => {
  await saveVoucher('POSTED')
}

// 保存凭证
const saveVoucher = async (status) => {
  if (!form.value.description) {
    return ElMessage.warning('请填写凭证摘要')
  }

  if (!isBalanced.value) {
    return ElMessage.error(`借贷不平衡！差额: ${difference.value.toFixed(2)}`)
  }

  const validSplits = splits.value.filter(s => s.accountId && s.amount > 0)
  if (validSplits.length < 2) {
    return ElMessage.warning('至少需要两条有效分录')
  }

  loading.value = true
  try {
    const res = await axios.post('http://localhost:8080/financeTransaction/add', {
      voucherDate: form.value.voucherDate,
      description: form.value.description,
      status: status,
      splits: validSplits.map(s => ({
        accountId: s.accountId,
        summary: s.summary || form.value.description,
        dcDirection: s.dcDirection,
        amount: s.amount
      }))
    })

    if (res.data.code === 200) {
      if (status === 'DRAFT') {
        ElMessage.success('草稿保存成功！请到「过账审核中心」审核')
      } else {
        ElMessage.success('凭证保存并过账成功！')
      }
      form.value.description = ''
      splits.value = [
        { accountId: null, summary: '', dcDirection: 1, quantity: 1, price: 0, amount: 0 },
        { accountId: null, summary: '', dcDirection: -1, quantity: 1, price: 0, amount: 0 }
      ]
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => loadAccountTree())
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>📝 凭证录入 (GnuCash 风格 - 支持多借多贷)</span>
        <div class="header-actions">
          <el-button type="success" plain @click="addDebitRow">+加借方</el-button>
          <el-button type="danger" plain @click="addCreditRow">+加贷方</el-button>
          <el-button @click="handleSaveDraft">📄 保存草稿</el-button>
          <el-button type="primary" @click="handleSaveAndPost">💾 保存并过账</el-button>
        </div>
      </div>
    </template>

    <div class="voucher-meta">
      <el-date-picker v-model="form.voucherDate" type="date" value-format="YYYY-MM-DD" style="width: 150px; margin-right: 15px;" :clearable="false" />
      <el-input v-model="form.description" placeholder="请输入业务总摘要（如：采购办公用品、销售商品、支付房租）" style="flex: 1;" />
    </div>

    <el-table :data="splits" border style="width: 100%">
      <el-table-column label="借/贷" width="70" align="center">
        <template #default="scope">
          <div class="direction-badge" :class="scope.row.dcDirection === 1 ? 'debit' : 'credit'" @click="toggleDirection(scope.row)">
            {{ scope.row.dcDirection === 1 ? '借' : '贷' }}
          </div>
        </template>
      </el-table-column>

      <el-table-column label="会计科目" min-width="200">
        <template #default="scope">
          <el-cascader v-model="scope.row.accountId" :options="accountOptions" :props="{ label: 'accountName', value: 'accountId', children: 'children', emitPath: false, checkStrictly: true }" placeholder="选择科目" filterable clearable style="width: 100%" />
        </template>
      </el-table-column>

      <el-table-column label="摘要" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.summary" :placeholder="form.description || '分录摘要'" />
        </template>
      </el-table-column>

      <el-table-column label="数量" width="80">
        <template #default="scope">
          <el-input-number v-model="scope.row.quantity" :min="1" :controls="false" style="width: 100%" @change="calcRowAmount(scope.row)" />
        </template>
      </el-table-column>

      <el-table-column label="单价" width="110">
        <template #default="scope">
          <el-input-number v-model="scope.row.price" :min="0" :precision="2" :controls="false" style="width: 100%" @change="calcRowAmount(scope.row)" @focus="autoFillBalance(scope.row)" />
        </template>
      </el-table-column>

      <el-table-column label="金额" width="110">
        <template #default="scope">
          <span class="amount-display">¥ {{ scope.row.amount.toFixed(2) }}</span>
        </template>
      </el-table-column>

      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-button type="danger" link :icon="Delete" @click="removeRow(scope.$index)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-bar">
      <div class="totals">
        <span>借方合计：<b class="debit-color">{{ totalDebit.toFixed(2) }}</b></span>
        <span class="divider">|</span>
        <span>贷方合计：<b class="credit-color">{{ totalCredit.toFixed(2) }}</b></span>
        <span class="divider">|</span>
        <span>差额：<b :class="isBalanced ? 'balanced' : 'unbalanced'">{{ difference.toFixed(2) }}</b></span>
      </div>
      <div class="status-tag" :class="isBalanced ? 'ok' : 'err'">{{ isBalanced ? '✓ 借贷平衡' : '✗ 不平衡' }}</div>
    </div>

    <div class="tips">
      <p>💡 <b>操作说明：</b></p>
      <ul>
        <li>点击「借」或「贷」可切换方向</li>
        <li>点击单价输入框时，会自动填入差额</li>
        <li><span class="tag-draft">保存草稿</span>：凭证暂存，需到「过账审核中心」审核</li>
        <li><span class="tag-post">保存并过账</span>：直接生效，立即更新报表</li>
      </ul>
    </div>
  </el-card>
</template>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; }
.voucher-meta { margin-bottom: 20px; background: #f8f9fa; padding: 15px; border-radius: 4px; display: flex; }
.direction-badge { display: inline-block; padding: 4px 12px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.direction-badge.debit { background: #e1f3d8; color: #67c23a; }
.direction-badge.credit { background: #fde2e2; color: #f56c6c; }
.amount-display { font-weight: bold; color: #409eff; }
.footer-bar { margin-top: 20px; display: flex; justify-content: space-between; align-items: center; padding: 15px; background: #fafafa; border-radius: 4px; }
.totals { display: flex; align-items: center; gap: 15px; font-size: 15px; }
.totals b { font-size: 17px; }
.debit-color { color: #67c23a; }
.credit-color { color: #f56c6c; }
.balanced { color: #67c23a; }
.unbalanced { color: #f56c6c; }
.divider { color: #ddd; }
.status-tag { padding: 6px 16px; border-radius: 20px; font-size: 14px; font-weight: bold; color: white; }
.status-tag.ok { background: #67C23A; }
.status-tag.err { background: #F56C6C; }
.tips { margin-top: 20px; padding: 15px; background: #ecf5ff; border-radius: 4px; font-size: 13px; color: #409eff; }
.tips ul { margin: 10px 0 0 20px; }
.tips li { margin: 5px 0; }
.tag-draft { background: #909399; color: white; padding: 1px 6px; border-radius: 3px; font-size: 12px; }
.tag-post { background: #409eff; color: white; padding: 1px 6px; border-radius: 3px; font-size: 12px; }
</style>