<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({
  bookName: '',
})

// 获取账簿列表
const getBookList = async () => {
  const res = await axios.get('/financeBook/list')
  tableData.value = res.data
}

// 点击新增按钮
const handleAdd = () => {
  form.value = { bookName: '' }
  dialogVisible.value = true
}

// === 关键修改：handleEnter 必须放在最外层，不能放在 submitForm 里面 ===
const handleEnter = (bookId) => {
  localStorage.setItem('current_book_id', bookId)
  // 跳转到科目管理页
  router.push('/home/accounts')
}
// ================================================================

// 提交新增
const submitForm = async () => {
  if (!form.value.bookName) {
    ElMessage.warning('请输入账簿名称')
    return
  }

  try {
    const res = await axios.post('/financeBook/add', form.value)
    if (res.data.code === 200) {
      ElMessage.success('创建成功！')
      dialogVisible.value = false
      getBookList()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  getBookList()
})
</script>

<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>📚 账簿管理</span>
        <el-button type="primary" @click="handleAdd">新建账套</el-button>
      </div>
    </template>

    <el-table :data="tableData" border style="width: 100%">
      <el-table-column prop="bookId" label="账簿ID" width="100" />
      <el-table-column prop="bookName" label="账簿名称" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" type="success" @click="handleEnter(scope.row.bookId)">
            进入账簿
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建账套" width="400px">
      <el-form :model="form">
        <el-form-item label="账簿名称">
          <el-input v-model="form.bookName" placeholder="例如：2025年第一季度账" />
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