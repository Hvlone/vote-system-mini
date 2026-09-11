import { createSSRApp } from 'vue'
import App from './App.vue'

// uni-app Vue3 固定入口写法：导出 createApp 工厂函数
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
