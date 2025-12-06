<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({ name: '', type: 'CUSTOMER', contactPerson: '', phone: '', address: '' })

// 加载数据
const loadData = async () => {
  // 请求后端真正的客户接口
  const res = await axios.get('http://localhost:8080/customer/list')
  tableData.value = res.data
}

const handleSave = async () => {
  await axios.post('http://localhost:8080/customer/save', form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除吗？').then(async () => {
    await axios.delete(`http://localhost:8080/customer/${id}`)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <h3>👥 往来单位管理</h3>
      <el-button type="primary" @click="dialogVisible = true; form={type:'CUSTOMER'}">+ 新增单位</el-button>
    </div>

    <el-table :data="tableData" border stripe style="margin-top: 20px;">
      <el-table-column prop="name" label="单位名称" min-width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 'CUSTOMER' ? 'success' : 'warning'">
            {{ scope.row.type === 'CUSTOMER' ? '客户' : '供应商' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contactPerson" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="danger" @click="handleDelete(scope.row.customerId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增往来单位" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="单位名称">
          <el-input v-model="form.name" placeholder="例如：xx 科技有限公司" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio label="CUSTOMER">客户 (应收)</el-radio>
            <el-radio label="VENDOR">供应商 (应付)</el-radio>
          </el-radio-group>
        </el-form-item>
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
.page-container { background: #fff; padding: 20px; border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>