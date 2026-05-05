import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 全局样式：CSS 变量 > 基础样式 > 响应式工具类
import '@/assets/styles/variables.css'
import '@/assets/styles/global.css'
import '@/assets/styles/responsive.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.mount('#app')
