<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const tableData = ref([])
const dialogVisible = ref(false)
const currentBookId = ref(null) // 当前正在操作哪本账？

// 表单数据
const form = ref({
  accountCode: '',
  accountName: '',
  accountType: 'ASSET' // 默认资产类
})

// 初始化：从浏览器缓存里拿出刚才点的账簿ID
const init = () => {
  const bookId = localStorage.getItem('current_book_id')
  if (!bookId) {
    ElMessage.warning('请先选择一个账簿！')
    router.push('/home/books') // 没选就踢回账簿列表
    return
  }
  currentBookId.value = bookId
  getAccountList()
}

// 获取科目列表
const getAccountList = async () => {
  // 发请求时带上 bookId
  const res = await axios.get(`http://localhost:8080/financeAccount/list?bookId=${currentBookId.value}`)
  tableData.value = res.data
}

// 提交新增
const submitForm = async () => {
  if (!form.value.accountCode || !form.value.accountName) {
    ElMessage.warning('请填写完整')
    return
  }

  // 加上账簿ID
  form.value.bookId = currentBookId.value

  try {
    const res = await axios.post('http://localhost:8080/financeAccount/add', form.value)
    if (res.data.code === 200) {
      ElMessage.success('添加成功！')
      dialogVisible.value = false
      getAccountList()
      // 清空表单
      form.value = { accountCode: '', accountName: '', accountType: 'ASSET' }
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  init()
})
</script>

<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>🗂️ 科目管理 (当前账簿ID: {{ currentBookId }})</span>
        <el-button type="primary" @click="dialogVisible = true">新增科目</el-button>
      </div>
    </template>

    <el-table :data="tableData" border style="width: 100%" row-key="accountId">
      <el-table-column prop="accountCode" label="科目代码" width="120" sortable />
      <el-table-column prop="accountName" label="科目名称" />
      <el-table-column prop="accountType" label="类型" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.accountType === 'ASSET'" type="success">资产</el-tag>
          <el-tag v-else-if="scope.row.accountType === 'LIABILITY'" type="danger">负债</el-tag>
          <el-tag v-else-if="scope.row.accountType === 'EQUITY'" type="warning">权益</el-tag>
          <el-tag v-else-if="scope.row.accountType === 'INCOME'">收入</el-tag>
          <el-tag v-else>费用</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增会计科目" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="科目代码">
          <el-input v-model="form.accountCode" placeholder="例如：1001" />
        </el-form-item>
        <el-form-item label="科目名称">
          <el-input v-model="form.accountName" placeholder="例如：库存现金" />
        </el-form-item>
        <el-form-item label="科目类型">
          <el-select v-model="form.accountType">
            <el-option label="资产 (ASSET)" value="ASSET" />
            <el-option label="负债 (LIABILITY)" value="LIABILITY" />
            <el-option label="权益 (EQUITY)" value="EQUITY" />
            <el-option label="收入 (INCOME)" value="INCOME" />
            <el-option label="费用 (EXPENSE)" value="EXPENSE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>