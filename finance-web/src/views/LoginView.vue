<script setup>
import { ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  try {
    const res = await axios.post('/sysUser/login', {
      username: form.value.username,
      password: form.value.password
    })

    if (res.data.code === 200) {
      ElMessage.success('登录成功！')

      // 1. 把用户信息简单存到浏览器里 (相当于发个通行证)
      localStorage.setItem('user_info', JSON.stringify(res.data.data))

      // 2. 【关键】跳转到主页 /home
      await router.push('/home')
    } else {
      ElMessage.error(res.data.msg || '登录失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('无法连接到服务器，请检查后端是否启动')
  }
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>财务管理系统</h2>
        </div>
      </template>

      <el-form :model="form" size="large">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleLogin">立即登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-card {
  width: 400px;
  border-radius: 8px;
}
.card-header {
  text-align: center;
}
</style>