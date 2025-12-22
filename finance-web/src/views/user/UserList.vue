<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 表格数据
const tableData = ref([])

// 弹窗控制开关
const dialogVisible = ref(false)

// 新增用户的表单数据
const form = ref({
  username: '',
  password: '',
  role: 'ACCOUNTANT' // 默认选会计
})

// 获取列表
const getUserList = async () => {
  const res = await axios.get('/sysUser/list')
  tableData.value = res.data
}

// 点击"添加用户"按钮
const handleAdd = () => {
  // 清空表单，打开弹窗
  form.value = { username: '', password: '', role: 'ACCOUNTANT' }
  dialogVisible.value = true
}

// 点击弹窗里的"确定"
const submitForm = async () => {
  if(!form.value.username) {
    ElMessage.warning('用户名不能为空')
    return
  }

  try {
    // 发送请求给后端
    const res = await axios.post('/sysUser/add', form.value)

    if (res.data.code === 200) {
      ElMessage.success('添加成功！')
      dialogVisible.value = false // 关掉弹窗
      getUserList() // 刷新表格，立刻看到新数据
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  getUserList()
})
</script>

<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>👤 用户列表</span>
        <el-button type="primary" @click="handleAdd">添加用户</el-button>
      </div>
    </template>

    <el-table :data="tableData" border style="width: 100%">
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          <el-tag v-if="scope.row.role === 'BOSS'" type="danger">老板</el-tag>
          <el-tag v-else-if="scope.row.role === 'ACCOUNTANT'">会计</el-tag>
          <el-tag v-else type="info">出纳</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="添加新用户" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="默认是 123456" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="老板 (BOSS)" value="BOSS" />
            <el-option label="会计 (ACCOUNTANT)" value="ACCOUNTANT" />
            <el-option label="出纳 (CASHIER)" value="CASHIER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
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