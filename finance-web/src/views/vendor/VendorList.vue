<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
// 默认类型固定为 VENDOR
const form = ref({ name: '', type: 'VENDOR', contactPerson: '', phone: '', address: '' })

const loadData = async () => {
  const res = await axios.get('http://localhost:8080/customer/list')
  // 【关键】前端过滤：只显示供应商
  tableData.value = res.data.filter(item => item.type === 'VENDOR')
}

const handleSave = async () => {
  form.value.type = 'VENDOR' // 强制设为供应商
  await axios.post('http://localhost:8080/customer/save', form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确认删除？').then(async () => {
    await axios.delete(`http://localhost:8080/customer/${id}`)
    loadData()
  })
}
onMounted(() => loadData())
</script>

<template>
  <div style="background:white; padding:20px;">
    <div style="display:flex; justify-content:space-between; margin-bottom:20px;">
      <h3>🚚 供应商列表 (我欠钱的人)</h3>
      <el-button type="primary" @click="dialogVisible=true; form={type:'VENDOR'}">新增供应商</el-button>
    </div>
    <el-table :data="tableData" border>
      <el-table-column prop="name" label="供应商名称" />
      <el-table-column prop="contactPerson" label="联系人" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column label="操作">
        <template #default="s"><el-button link type="danger" @click="handleDelete(s.row.customerId)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增供应商" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="如：XX文具店" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>