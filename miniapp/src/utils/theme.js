/**
 * 暗夜模式：主题状态 + 本地持久化 + 原生栏同步
 *
 * 约定：
 *  - 主题只有「暗 / 亮」两态，首次启动默认暗色（本地无记录即视为开启）
 *  - 页面底色与文字色由各页根节点的 .theme-dark 类驱动，CSS 变量定义在 App.vue
 *  - 小程序导航栏 / 下拉刷新窗口背景不受 CSS 控制，需调用原生 API 同步
 *  - H5 导航栏由根元素（html）上的 theme-dark 标记 + App.vue 的全局 CSS 驱动：
 *    uni.setNavigationBarColor 在 H5 只作用于页面栈末位，页面被复用时会写错对象，
 *    导致当前页导航栏停留在旧颜色
 */
import { reactive } from 'vue'

// 本地存储键：true=暗夜模式开启，false=亮色
const STORAGE_KEY = 'theme_dark'

// 全局共享的主题状态，各页面通过 useTheme() 读取
const themeState = reactive({ dark: true })

/**
 * 同步原生栏外观（导航栏 + 下拉刷新窗口背景）
 * @param {Boolean} dark 是否暗夜模式
 */
export function applyNativeTheme(dark) {
  uni.setNavigationBarColor({
    frontColor: dark ? '#ffffff' : '#000000',
    backgroundColor: dark ? '#000000' : '#ffffff'
  })
  // 下拉刷新窗口背景与 loading 点色：H5 未实现这两个 API（typeof 为 undefined），
  // 需按能力调用，否则会抛 TypeError 并中断后续调用
  if (typeof uni.setBackgroundColor === 'function') {
    uni.setBackgroundColor({
      backgroundColor: dark ? '#000000' : '#F5F6F8'
    })
  }
  if (typeof uni.setBackgroundTextStyle === 'function') {
    uni.setBackgroundTextStyle({
      textStyle: dark ? 'light' : 'dark'
    })
  }
  // H5：导航栏与画布底色改由根元素标记 + 全局 CSS 驱动，可即时切换且不受页面栈顺序影响
  // #ifdef H5
  document.documentElement.classList.toggle('theme-dark', dark)
  // #endif
}

/** 启动时读取本地设置：无记录 → 默认开启暗夜模式 */
export function initTheme() {
  const saved = uni.getStorageSync(STORAGE_KEY)
  themeState.dark = saved === '' || saved === null || saved === undefined ? true : !!saved
  applyNativeTheme(themeState.dark)
}

/**
 * 切换主题并持久化
 * @param {Boolean} enabled 是否开启暗夜模式
 */
export function setDark(enabled) {
  themeState.dark = !!enabled
  uni.setStorageSync(STORAGE_KEY, themeState.dark)
  applyNativeTheme(themeState.dark)
}

/** 供页面读取响应式主题状态 */
export function useTheme() {
  return themeState
}
