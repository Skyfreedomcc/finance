import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 1. 引入 axios
import axios from 'axios'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

// 2. 【关键修改】设置全局的基础请求路径
// 意思是：以后只要你用 axios 发请求，自动在前面加上 /api
// 这样 Nginx 就能识别并转发了
axios.defaults.baseURL = '/api'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')