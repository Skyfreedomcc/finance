<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
// 默认类型固定为 VENDOR (供应商)
const form = ref({ name: '', type: 'VENDOR', contactPerson: '', phone: '', address: '' })

// 加载数据
const loadData = async () => {
  try {
    const res = await axios.get('http://localhost:8080/customer/list')
    // 【关键逻辑】只显示 type 为 VENDOR 的数据
    tableData.value = (res.data || []).filter(item => item.type === 'VENDOR')
  } catch (err) {
    ElMessage.error('无法连接后端，请检查服务是否启动')
  }
}

// 保存数据
const handleSave = async () => {
  if (!form.value.name) return ElMessage.warning('必须填写供应商名称')

  try {
    form.value.type = 'VENDOR' // 确保类型正确
    // 【关键接口】指向 /customer/save
    await axios.post('http://localhost:8080/customer/save', form.value)

    ElMessage.success('供应商保存成功！')
    dialogVisible.value = false
    loadData()
  } catch (err) {
    console.error(err)
    ElMessage.error('保存失败，请按F12查看错误信息')
  }
}

// 删除数据
const handleDelete = (id) => {
  ElMessageBox.confirm('确定不再合作，删除该供应商？', '警告', {
    confirmButtonText: '删除',
    type: 'warning'
  }).then(async () => {
    await axios.delete(`http://localhost:8080/customer/${id}`)
    ElMessage.success('已删除')
    loadData()
  })
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-box">
    <div class="header">
      <h3>供应商管理</h3>
      <el-button type="primary" @click="dialogVisible = true; form={type:'VENDOR'}">+ 新增供应商</el-button>
    </div>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="name" label="供应商名称" />
      <el-table-column prop="contactPerson" label="联系人" width="120" />
      <el-table-column prop="phone" label="电话" width="150" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="danger" @click="handleDelete(scope.row.customerId)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div style="padding: 30px; text-align: center; color: #999;">暂无数据，请添加。</div>
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增供应商" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.name" placeholder="如：XX原材料厂" /></el-form-item>
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