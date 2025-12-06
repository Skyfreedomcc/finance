<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 接收路由参数 (采购 PURCHASE / 销售 SALE)
const props = defineProps(['type'])
const billType = ref(props.type || 'PURCHASE')
const isLocked = computed(() => !!props.type)

const form = ref({
  partnerId: null,
  date: new Date().toISOString().split('T')[0],
  // 【改动点】增加了 price 和 quantity 字段
  rows: [{ accountId: null, memo: '', price: 0, quantity: 1, amount: 0 }]
})

const partners = ref([])
const accounts = ref([])

const initData = async () => {
  const res1 = await axios.get('http://localhost:8080/customer/list')
  partners.value = res1.data
  const res2 = await axios.get('http://localhost:8080/financeAccount/list')
  accounts.value = res2.data
}

// 自动计算行金额
const calcRowAmount = (row) => {
  row.amount = Number(row.price || 0) * Number(row.quantity || 0)
}

// 计算整单总金额
const totalBillAmount = computed(() => {
  return form.value.rows.reduce((sum, r) => sum + r.amount, 0)
})

const submitBill = async () => {
  if(!form.value.partnerId) return ElMessage.warning('请选择往来单位')
  if(totalBillAmount.value <= 0) return ElMessage.warning('单据总金额不能为0')

  const transaction = {
    description: billType.value === 'PURCHASE' ? '采购入库' : '销售出库',
    voucherDate: form.value.date,
    splits: []
  }

  // 1. 处理明细行 (商品/费用)
  form.value.rows.forEach(row => {
    if(row.amount > 0 && row.accountId) {
      transaction.splits.push({
        accountId: row.accountId,
        // 把单价数量写在摘要里，方便以后查
        summary: `${row.memo} (单价:${row.price} * 数量:${row.quantity})`,
        amount: row.amount,
        dcDirection: billType.value === 'PURCHASE' ? 1 : -1
      })
    }
  })

  // 2. 自动对冲 (应付/应收)
  const targetName = billType.value === 'PURCHASE' ? '应付账款' : '应收账款'
  const targetAcc = accounts.value.find(a => a.accountName.includes(targetName))

  if (targetAcc) {
    transaction.splits.push({
      accountId: targetAcc.accountId,
      summary: '系统自动挂账',
      amount: totalBillAmount.value,
      dcDirection: billType.value === 'PURCHASE' ? -1 : 1
    })
  } else {
    return ElMessage.error(`系统找不到“${targetName}”科目，请检查科目表！`)
  }

  await axios.post('http://localhost:8080/financeTransaction/add', transaction)
  ElMessage.success('单据保存成功！已自动过账。')
  // 重置表单
  form.value.rows = [{ accountId: null, memo: '', price: 0, quantity: 1, amount: 0 }]
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
      <el-radio-group v-if="!isLocked" v-model="billType">
        <el-radio-button label="PURCHASE">采购单</el-radio-button>
        <el-radio-button label="SALE">销售单</el-radio-button>
      </el-radio-group>
      <h2 v-else style="margin:0;">
        {{ billType === 'PURCHASE' ? '📦 新建采购账单' : '💰 新建销售发票' }}
      </h2>
      <div style="margin-top:10px;">
        <span style="color:#666">业务日期：</span>
        <el-date-picker v-model="form.date" value-format="YYYY-MM-DD" style="width:150px" />
      </div>
    </div>

    <div class="form-area">
      <el-form label-width="80px">
        <el-form-item :label="billType==='PURCHASE'?'供应商':'客户'">
          <el-select v-model="form.partnerId" placeholder="请选择..." style="width:300px">
            <el-option v-for="p in currentPartners" :key="p.customerId" :label="p.name" :value="p.customerId" />
          </el-select>
        </el-form-item>
      </el-form>

      <table class="simple-table">
        <thead>
        <tr>
          <th width="25%">科目</th>
          <th width="25%">摘要 (品名)</th>
          <th width="15%">单价</th>
          <th width="10%">数量</th>
          <th width="15%">小计金额</th>
          <th width="10%">操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(row, idx) in form.rows" :key="idx">
          <td>
            <el-select v-model="row.accountId" filterable placeholder="选科目..." style="width:100%">
              <el-option v-for="acc in accounts" :key="acc.accountId" :label="acc.accountCode+' '+acc.accountName" :value="acc.accountId" />
            </el-select>
          </td>
          <td><el-input v-model="row.memo" placeholder="例如：A4纸" /></td>

          <td>
            <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width:100%" @change="calcRowAmount(row)" />
          </td>

          <td>
            <el-input-number v-model="row.quantity" :min="1" :step="1" :controls="false" style="width:100%" @change="calcRowAmount(row)" />
          </td>

          <td>
            <el-input v-model="row.amount" disabled>
              <template #prefix>¥</template>
            </el-input>
          </td>

          <td style="text-align:center">
            <el-button link type="danger" @click="form.rows.splice(idx, 1)">删除</el-button>
          </td>
        </tr>
        </tbody>
      </table>

      <div class="actions">
        <el-button @click="form.rows.push({price:0, quantity:1, amount:0})">+ 加一行</el-button>
        <div class="total-bar">
          总金额: <span class="big-money">¥ {{ totalBillAmount.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" @click="submitBill">保存并过账</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bill-page { background: #fff; padding: 20px; border-radius: 8px; }
.header { border-bottom: 1px solid #eee; padding-bottom: 20px; margin-bottom: 20px; }
.simple-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
.simple-table th { background: #f5f7fa; padding: 10px; text-align: left; border: 1px solid #ebeef5; }
.simple-table td { padding: 5px; border: 1px solid #ebeef5; }
.actions { display: flex; align-items: center; justify-content: space-between; }
.total-bar { font-size: 16px; font-weight: bold; }
.big-money { color: #F56C6C; font-size: 24px; margin-left: 10px; }
</style>