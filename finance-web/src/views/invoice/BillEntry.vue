<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 接收路由传来的 type 参数 (PURCHASE 或 SALE)
const props = defineProps(['type'])

// 核心逻辑：如果路由传了类型，就锁定类型；没传（比如通用入口），就默认采购
const isLocked = computed(() => !!props.type)
const billType = ref(props.type || 'PURCHASE')

const form = ref({
  partnerId: null,
  date: new Date().toISOString().split('T')[0],
  rows: [{ accountId: null, memo: '', amount: 0 }]
})

const partners = ref([])
const accounts = ref([])

const initData = async () => {
  const res1 = await axios.get('http://localhost:8080/customer/list')
  partners.value = res1.data
  const res2 = await axios.get('http://localhost:8080/financeAccount/list')
  accounts.value = res2.data
}

// ... (submitBill 函数保持不变，为了节省篇幅省略，请保留原有的逻辑) ...
// 必须把之前的 submitBill 函数逻辑完整保留在这里！
const submitBill = async () => {
  if(!form.value.partnerId) return ElMessage.warning('请选择往来单位')
  const transaction = {
    description: billType.value === 'PURCHASE' ? '采购账单' : '销售发票',
    voucherDate: form.value.date,
    splits: []
  }
  let total = 0
  form.value.rows.forEach(row => {
    if(row.amount > 0 && row.accountId) {
      total += Number(row.amount)
      transaction.splits.push({
        accountId: row.accountId, summary: row.memo, amount: row.amount,
        dcDirection: billType.value === 'PURCHASE' ? 1 : -1
      })
    }
  })
  const targetName = billType.value === 'PURCHASE' ? '应付账款' : '应收账款'
  const targetAcc = accounts.value.find(a => a.accountName.includes(targetName))
  if (targetAcc) {
    transaction.splits.push({
      accountId: targetAcc.accountId, summary: '系统自动挂账', amount: total,
      dcDirection: billType.value === 'PURCHASE' ? -1 : 1
    })
  } else {
    return ElMessage.error(`找不到“${targetName}”科目`)
  }
  await axios.post('http://localhost:8080/financeTransaction/add', transaction)
  ElMessage.success('单据已入账！')
  form.value.rows = [{ accountId: null, memo: '', amount: 0 }]
}

const currentPartners = computed(() => {
  const typeKey = billType.value === 'PURCHASE' ? 'VENDOR' : 'CUSTOMER'
  return partners.value.filter(p => p.type === typeKey)
})

onMounted(() => initData())
</script>

<template>
  <div class="bill-page">
    <div class="header">
      <el-radio-group v-if="!isLocked" v-model="billType" size="large">
        <el-radio-button label="PURCHASE">收到账单 (供应商)</el-radio-button>
        <el-radio-button label="SALE">开出发票 (客户)</el-radio-button>
      </el-radio-group>

      <h2 v-else style="margin:0;">
        {{ billType === 'PURCHASE' ? '📄 新建采购账单 (入库)' : '📄 新建销售发票 (出库)' }}
      </h2>
    </div>

    <div class="form-area">
      <el-form label-width="100px">
        <el-form-item :label="billType==='PURCHASE'?'供应商':'客户'">
          <el-select v-model="form.partnerId" placeholder="请选择..." style="width:300px">
            <el-option v-for="p in currentPartners" :key="p.customerId" :label="p.name" :value="p.customerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务日期">
          <el-date-picker v-model="form.date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>

      <table class="simple-table">
        <thead>
        <tr>
          <th width="30%">科目 ({{ billType==='PURCHASE'?'支出项':'收入项' }})</th>
          <th width="40%">摘要</th>
          <th width="20%">金额</th>
          <th width="10%"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(row, idx) in form.rows" :key="idx">
          <td>
            <el-select v-model="row.accountId" filterable placeholder="搜科目...">
              <el-option v-for="acc in accounts" :key="acc.accountId" :label="acc.accountCode+' '+acc.accountName" :value="acc.accountId" />
            </el-select>
          </td>
          <td><el-input v-model="row.memo" /></td>
          <td><el-input-number v-model="row.amount" :min="0" :controls="false" style="width:100%" /></td>
          <td><el-button link type="danger" @click="form.rows.splice(idx, 1)">删除</el-button></td>
        </tr>
        </tbody>
      </table>

      <div class="actions">
        <el-button @click="form.rows.push({amount:0})">+ 加一行</el-button>
        <el-button type="primary" size="large" @click="submitBill" style="margin-left: 20px;">保存单据</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bill-page { background: #fff; padding: 20px; }
.header { margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 20px; }
.simple-table { width: 100%; border-collapse: collapse; margin: 10px 0 20px 0; }
.simple-table th { text-align: left; background: #f5f7fa; padding: 10px; color: #666; }
.simple-table td { padding: 5px; border-bottom: 1px solid #eee; }
</style>