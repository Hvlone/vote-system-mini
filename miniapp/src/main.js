import { createSSRApp } from 'vue'
import App from './App.vue'
import { applyNativeTheme, useTheme } from './utils/theme'

// uni-app Vue3 固定入口写法：导出 createApp 工厂函数
export function createApp() {
  const app = createSSRApp(App)

  // 全局混入：每个页面显示时同步一次原生导航栏/窗口外观（暗夜模式只靠 CSS 管不到原生栏）
  app.mixin({
    onShow() {
      applyNativeTheme(useTheme().dark)
    }
  })

  return { app }
}
