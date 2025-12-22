<script setup>
/**
 * 采购/销售单录入 - 统一审核版
 *
 * 改进：只保存草稿，统一在过账审核中心审核
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps(['type'])
const billType = ref(props.type || 'PURCHASE')

const form = ref({
  partnerId: null,
  date: new Date().toISOString().split('T')[0],
  description: ''
})

const items = ref([
  { accountId: null, memo: '', price: 0, quantity: 1, amount: 0 }
])

const partners = ref([])
const accounts = ref([])
const accountTree = ref([])

const initData = async () => {
  try {
    const [partnerRes, accountRes] = await Promise.all([
      axios.get('/customer/list'),
      axios.get('/financeAccount/list')
    ])
    partners.value = partnerRes.data || []
    accounts.value = accountRes.data || []
    accountTree.value = handleTree(accountRes.data || [], "accountId", "parentId")
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

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

const calcRowAmount = (row) => {
  row.amount = Number(row.price || 0) * Number(row.quantity || 0)
}

const addRow = () => {
  items.value.push({ accountId: null, memo: '', price: 0, quantity: 1, amount: 0 })
}

const removeRow = (index) => {
  if (items.value.length <= 1) {
    ElMessage.warning('至少保留一行')
    return
  }
  items.value.splice(index, 1)
}

const totalAmount = computed(() => {
  return items.value.reduce((sum, r) => sum + Number(r.amount || 0), 0)
})

const pageTitle = computed(() => {
  return billType.value === 'PURCHASE' ? '🛒 新建采购账单' : '💰 新建销售发票'
})

const currentPartners = computed(() => {
  const typeKey = billType.value === 'PURCHASE' ? 'VENDOR' : 'CUSTOMER'
  return partners.value.filter(p => p.type === typeKey || !p.type)
})

// 会计分录预览
const splitsPreview = computed(() => {
  const splits = []

  if (billType.value === 'PURCHASE') {
    items.value.forEach(item => {
      if (item.amount > 0 && item.accountId) {
        const acc = accounts.value.find(a => a.accountId === item.accountId)
        splits.push({
          direction: '借', dirClass: 'debit',
          accountName: acc ? acc.accountName : '未选择',
          summary: item.memo || '商品',
          amount: item.amount
        })
      }
    })
    if (totalAmount.value > 0) {
      splits.push({
        direction: '贷', dirClass: 'credit',
        accountName: '应付账款',
        summary: '应付供应商',
        amount: totalAmount.value
      })
    }
  } else {
    if (totalAmount.value > 0) {
      splits.push({
        direction: '借', dirClass: 'debit',
        accountName: '应收账款',
        summary: '应收客户款',
        amount: totalAmount.value
      })
    }
    items.value.forEach(item => {
      if (item.amount > 0 && item.accountId) {
        const acc = accounts.value.find(a => a.accountId === item.accountId)
        splits.push({
          direction: '贷', dirClass: 'credit',
          accountName: acc ? acc.accountName : '未选择',
          summary: item.memo || '销售',
          amount: item.amount
        })
      }
    })
  }

  return splits
})

// 保存草稿
const submitAsDraft = async () => {
  await submitBill('DRAFT')
}

// 保存并过账
const submitAndPost = async () => {
  await submitBill('POSTED')
}

// 提交保存
const submitBill = async (status) => {
  if (!form.value.partnerId) {
    return ElMessage.warning('请选择往来单位')
  }
  if (totalAmount.value <= 0) {
    return ElMessage.warning('单据金额不能为0')
  }

  const description = billType.value === 'PURCHASE'
    ? `采购入库 - ${form.value.description || ''}`
    : `销售出库 - ${form.value.description || ''}`

  const splits = []

  if (billType.value === 'PURCHASE') {
    items.value.forEach(item => {
      if (item.amount > 0 && item.accountId) {
        splits.push({
          accountId: item.accountId,
          summary: `${item.memo} (${item.price}×${item.quantity})`,
          amount: item.amount,
          dcDirection: 1
        })
      }
    })

    const apAccount = accounts.value.find(a => a.accountName && a.accountName.includes('应付账款'))
    if (apAccount) {
      splits.push({
        accountId: apAccount.accountId,
        summary: '应付供应商',
        amount: totalAmount.value,
        dcDirection: -1
      })
    } else {
      return ElMessage.error('找不到"应付账款"科目！')
    }
  } else {
    const arAccount = accounts.value.find(a => a.accountName && a.accountName.includes('应收账款'))
    if (arAccount) {
      splits.push({
        accountId: arAccount.accountId,
        summary: '应收客户款',
        amount: totalAmount.value,
        dcDirection: 1
      })
    } else {
      return ElMessage.error('找不到"应收账款"科目！')
    }

    items.value.forEach(item => {
      if (item.amount > 0 && item.accountId) {
        splits.push({
          accountId: item.accountId,
          summary: `${item.memo} (${item.price}×${item.quantity})`,
          amount: item.amount,
          dcDirection: -1
        })
      }
    })
  }

  try {
    const res = await axios.post('/financeTransaction/add', {
      voucherDate: form.value.date,
      description: description,
      status: status,
      splits: splits
    })

    if (res.data.code === 200) {
      if (status === 'DRAFT') {
        ElMessage.success('草稿保存成功！请到「过账审核中心」审核')
      } else {
        ElMessage.success('保存并过账成功！')
      }
      form.value.partnerId = null
      form.value.description = ''
      items.value = [{ accountId: null, memo: '', price: 0, quantity: 1, amount: 0 }]
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '网络错误'))
  }
}

onMounted(() => initData())
</script>

<template>
  <div class="bill-page">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <div class="header-meta">
        <span>业务日期：</span>
        <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>📦 {{ billType === 'PURCHASE' ? '采购' : '销售' }}明细</span>
              <el-button type="primary" plain size="small" @click="addRow">+加一行</el-button>
            </div>
          </template>

          <el-form label-width="80px" style="margin-bottom: 15px;">
            <el-form-item :label="billType === 'PURCHASE' ? '供应商' : '客户'">
              <el-select v-model="form.partnerId" placeholder="请选择..." style="width: 100%">
                <el-option v-for="p in currentPartners" :key="p.customerId" :label="p.name" :value="p.customerId" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.description" placeholder="单据备注（可选）" />
            </el-form-item>
          </el-form>

          <el-alert
            :title="billType === 'PURCHASE' ? '请选择资产或费用科目（如：库存商品、原材料、办公费）' : '请选择收入科目（如：主营业务收入）'"
            type="info" :closable="false" style="margin-bottom: 15px;"
          />

          <el-table :data="items" border size="small">
            <el-table-column :label="billType === 'PURCHASE' ? '科目(资产/费用)' : '科目(收入)'" min-width="160">
              <template #default="scope">
                <el-cascader v-model="scope.row.accountId" :options="accountTree" :props="{ label: 'accountName', value: 'accountId', children: 'children', emitPath: false, checkStrictly: true }" placeholder="选科目" filterable size="small" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="品名/摘要" min-width="100">
              <template #default="scope">
                <el-input v-model="scope.row.memo" placeholder="A4纸" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100">
              <template #default="scope">
                <el-input-number v-model="scope.row.price" :min="0" :precision="2" :controls="false" size="small" style="width: 100%" @change="calcRowAmount(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="80">
              <template #default="scope">
                <el-input-number v-model="scope.row.quantity" :min="1" :controls="false" size="small" style="width: 100%" @change="calcRowAmount(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="scope">
                <span class="subtotal">¥{{ scope.row.amount.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column width="45" align="center">
              <template #default="scope">
                <el-button type="danger" link size="small" :icon="Delete" @click="removeRow(scope.$index)" />
              </template>
            </el-table-column>
          </el-table>

          <div class="total-row">
            <span>合计金额：</span>
            <span class="total-amount">¥ {{ totalAmount.toFixed(2) }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="splits-preview">
          <template #header>📊 会计分录预览</template>

          <el-alert
            :title="billType === 'PURCHASE' ? '采购：借-资产/费用增加，贷-应付账款增加' : '销售：借-应收账款增加，贷-收入增加'"
            type="success" :closable="false" style="margin-bottom: 15px;"
          />

          <div class="splits-list" v-if="splitsPreview.length > 0">
            <div class="split-row" v-for="(split, idx) in splitsPreview" :key="idx">
              <span class="dir-tag" :class="split.dirClass">{{ split.direction }}</span>
              <span class="acc-name">{{ split.accountName }}</span>
              <span class="acc-amount">¥{{ split.amount.toFixed(2) }}</span>
            </div>
          </div>
          <div v-else class="empty-tip">请在左侧填写明细</div>

          <div class="action-buttons">
            <el-button size="large" style="flex: 1;" @click="submitAsDraft">
              📄 保存草稿
            </el-button>
            <el-button type="primary" size="large" style="flex: 1;" @click="submitAndPost">
              💾 保存并过账
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.bill-page { padding: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.page-header h2 { margin: 0; }
.header-meta { display: flex; align-items: center; gap: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.subtotal { color: #409EFF; font-weight: bold; }
.total-row { display: flex; justify-content: flex-end; align-items: center; gap: 10px; margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; font-size: 16px; }
.total-amount { font-size: 24px; font-weight: bold; color: #f56c6c; }
.splits-preview { background: #fafafa; }
.splits-list { border: 1px solid #eee; border-radius: 4px; overflow: hidden; }
.split-row { display: flex; align-items: center; padding: 10px; background: white; border-bottom: 1px solid #eee; }
.split-row:last-child { border-bottom: none; }
.dir-tag { padding: 3px 10px; border-radius: 4px; font-weight: bold; font-size: 12px; margin-right: 10px; }
.dir-tag.debit { background: #e1f3d8; color: #67c23a; }
.dir-tag.credit { background: #fde2e2; color: #f56c6c; }
.acc-name { flex: 1; }
.acc-amount { font-weight: bold; }
.empty-tip { text-align: center; padding: 40px; color: #999; }
.action-buttons { display: flex; gap: 10px; margin-top: 20px; }
</style>