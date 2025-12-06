import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

// === 新增部分开始：导入 Element Plus ===
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// === 新增部分结束 ===

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// === 新增部分开始：使用 Element Plus ===
app.use(ElementPlus)
// === 新增部分结束 ===

app.mount('#app')