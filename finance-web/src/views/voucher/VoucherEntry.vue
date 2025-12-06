<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentBookId = ref(null)
const accountList = ref([]) // 所有的科目选项

// 凭证主信息
const form = ref({
  voucherDate: new Date().toISOString().split('T')[0], // 默认今天
  description: ''
})

// 分录行（默认给两行：一借一贷）
const splits = ref([
  { accountId: '', summary: '', dcDirection: 1, amount: 0 },
  { accountId: '', summary: '', dcDirection: -1, amount: 0 }
])

// 初始化：获取账簿ID和科目列表
const init = async () => {
  const bookId = localStorage.getItem('current_book_id')
  if (!bookId) {
    ElMessage.warning('请先选择账簿')
    router.push('/home/books')
    return
  }
  currentBookId.value = bookId

  // 加载该账簿下的所有科目，供下拉选择
  const res = await axios.get(`http://localhost:8080/financeAccount/list?bookId=${bookId}`)
  accountList.value = res.data
}

// 增加一行
const addRow = () => {
  splits.value.push({ accountId: '', summary: form.value.description, dcDirection: 1, amount: 0 })
}

// 删除一行
const removeRow = (index) => {
  splits.value.splice(index, 1)
}

// 计算借方合计
const totalDebit = computed(() => {
  return splits.value
    .filter(row => row.dcDirection === 1)
    .reduce((sum, row) => sum + Number(row.amount), 0)
})

// 计算贷方合计
const totalCredit = computed(() => {
  return splits.value
    .filter(row => row.dcDirection === -1)
    .reduce((sum, row) => sum + Number(row.amount), 0)
})

// 保存凭证
const handleSave = async () => {
  // 1. 简单校验
  if (totalDebit.value !== totalCredit.value) {
    ElMessage.error(`借贷不平衡！借:${totalDebit.value} 贷:${totalCredit.value}`)
    return
  }
  if (!form.value.description) {
    ElMessage.warning('请填写凭证摘要')
    return
  }

  // 2. 组装数据
  const postData = {
    bookId: currentBookId.value,
    voucherDate: form.value.voucherDate,
    description: form.value.description,
    splits: splits.value
  }

  // 3. 发送请求
  try {
    const res = await axios.post('http://localhost:8080/financeTransaction/add', postData)
    if (res.data.code === 200) {
      ElMessage.success('保存成功！')
      // 清空数据，准备记下一笔
      form.value.description = ''
      splits.value = [
        { accountId: '', summary: '', dcDirection: 1, amount: 0 },
        { accountId: '', summary: '', dcDirection: -1, amount: 0 }
      ]
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  init()
})
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <h3>📝 凭证录入</h3>
        <div>
          <el-button @click="addRow">➕ 加一行</el-button>
          <el-button type="primary" size="large" @click="handleSave">💾 保存凭证</el-button>
        </div>
      </div>
    </template>

    <el-form :inline="true" class="voucher-head">
      <el-form-item label="日期">
        <el-date-picker v-model="form.voucherDate" type="date" placeholder="选择日期" />
      </el-form-item>
      <el-form-item label="总摘要">
        <el-input v-model="form.description" placeholder="例如：提现、报销差旅费" style="width: 300px" />
      </el-form-item>
    </el-form>

    <el-table :data="splits" border style="width: 100%">
      <el-table-column label="摘要">
        <template #default="scope">
          <el-input v-model="scope.row.summary" :placeholder="form.description || '同上'" />
        </template>
      </el-table-column>

      <el-table-column label="会计科目" width="220">
        <template #default="scope">
          <el-select v-model="scope.row.accountId" placeholder="选择科目" filterable>
            <el-option
              v-for="item in accountList"
              :key="item.accountId"
              :label="item.accountCode + ' ' + item.accountName"
              :value="item.accountId"
            />
          </el-select>
        </template>
      </el-table-column>

      <el-table-column label="借/贷" width="100">
        <template #default="scope">
          <el-select v-model="scope.row.dcDirection">
            <el-option label="借" :value="1" />
            <el-option label="贷" :value="-1" />
          </el-select>
        </template>
      </el-table-column>

      <el-table-column label="金额" width="180">
        <template #default="scope">
          <el-input v-model="scope.row.amount" type="number" />
        </template>
      </el-table-column>

      <el-table-column width="60">
        <template #default="scope">
          <el-button type="danger" circle size="small" @click="removeRow(scope.$index)">×</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-total">
      <span>借方合计: <strong style="color: green">{{ totalDebit }}</strong></span>
      <span style="margin-left: 20px">贷方合计: <strong style="color: red">{{ totalCredit }}</strong></span>
      <span style="margin-left: 20px; font-size: 12px; color: #999" v-if="totalDebit !== totalCredit">❌ 不平衡</span>
      <span style="margin-left: 20px; font-size: 12px; color: green" v-else>✅ 平衡</span>
    </div>

  </el-card>
</template>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.voucher-head { background: #f9f9f9; padding: 20px; margin-bottom: 20px; }
.footer-total { margin-top: 20px; text-align: right; font-size: 18px; }
</style>