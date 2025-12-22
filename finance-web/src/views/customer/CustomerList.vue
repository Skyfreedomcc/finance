<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({ name: '', type: 'CUSTOMER', contactPerson: '', phone: '', address: '' })

const loadData = async () => {
  try {
    const res = await axios.get('/customer/list')
    // 【关键逻辑】只显示 type 为 CUSTOMER 的数据
    tableData.value = (res.data || []).filter(item => item.type === 'CUSTOMER')
  } catch (err) {
    ElMessage.error('无法连接后端')
  }
}

const handleSave = async () => {
  if (!form.value.name) return ElMessage.warning('请输入客户名称')

  try {
    form.value.type = 'CUSTOMER' // 强制标记为客户
    await axios.post('/customer/save', form.value)

    ElMessage.success('客户保存成功！')
    dialogVisible.value = false
    loadData()
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该客户？', '警告', { type: 'warning' }).then(async () => {
    await axios.delete(`/customer/${id}`)
    loadData()
  })
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-box">
    <div class="header">
      <h3>客户管理 </h3>
      <el-button type="primary" @click="dialogVisible = true; form={type:'CUSTOMER'}">+ 新增客户</el-button>
    </div>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="name" label="客户名称" />
      <el-table-column prop="contactPerson" label="联系人" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="danger" @click="handleDelete(scope.row.customerId)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><div style="padding:30px; text-align:center; color:#999">暂无客户</div></template>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增客户" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.name" placeholder="如：张三 / 某某公司" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-box { background: white; padding: 20px; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; }
</style>