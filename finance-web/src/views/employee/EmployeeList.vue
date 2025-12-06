<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const dialogVisible = ref(false)
const loading = ref(false)
const form = ref({ name: '', position: '', phone: '', basicSalary: 0 })

// 1. 加载员工列表
const load = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/employee/list')
    list.value = res.data || []
  } catch (err) {
    console.error(err)
    ElMessage.error('无法加载列表，请检查后端是否启动！')
  } finally {
    loading.value = false
  }
}

// 2. 保存员工 (核心修复)
const save = async () => {
  // 简单校验
  if(!form.value.name) return ElMessage.warning('请输入员工姓名！')

  try {
    // 【关键】这里请求的是 /employee/save，不是 sysUser
    console.log("正在提交数据...", form.value)
    const res = await axios.post('http://localhost:8080/employee/save', form.value)

    // 如果后端返回 true 或 成功状态
    if (res.data) {
      ElMessage.success('员工保存成功！')
      dialogVisible.value = false
      load() // 刷新列表
    } else {
      ElMessage.error('保存失败，后端返回错误')
    }
  } catch (error) {
    console.error(error)
    // 【关键】如果这里弹窗，说明是网络问题或跨域问题
    ElMessage.error('请求发送失败！请按 F12 查看控制台红色报错。')
  }
}

// 3. 删除员工
const del = (id) => {
  ElMessageBox.confirm('确定要开除这名员工吗？', '警告', {
    confirmButtonText: '确定删除',
    type: 'warning'
  }).then(async () => {
    await axios.delete(`http://localhost:8080/employee/${id}`)
    ElMessage.success('已删除')
    load()
  })
}

onMounted(() => load())
</script>

<template>
  <div class="page-box">
    <div class="header">
      <h3>👨‍💼 员工花名册 (Employee Roster)</h3>
      <el-button type="primary" @click="dialogVisible=true; form={basicSalary: 3000}">+ 新增员工</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="position" label="职位" width="150" />
      <el-table-column prop="phone" label="联系电话" width="180" />
      <el-table-column prop="basicSalary" label="基本工资 (元)">
        <template #default="scope">
          <span style="color: #67C23A; font-weight: bold;">¥ {{ scope.row.basicSalary }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link type="danger" @click="del(scope.row.employeeId)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div style="padding: 30px; text-align: center; color: #999;">
          暂无员工，请点击右上角按钮添加。
        </div>
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" title="录入新员工" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.position" placeholder="例如：收银员 / 库管" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="基本工资">
          <el-input-number v-model="form.basicSalary" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-box { background: white; padding: 20px; border-radius: 8px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 15px;}
</style>