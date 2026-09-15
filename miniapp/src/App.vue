<script>
import { initTheme } from './utils/theme'

export default {
  onLaunch() {
    // 启动时不做跳转：无 token 的登录判断统一放在登录页 onLoad，避免首屏竞态
    console.log('App Launch')
    // 读取本地主题设置并同步原生栏（首次启动默认暗夜模式）
    initTheme()
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style>
/* ========== 主题变量（亮色为基准值，暗夜模式由各页根节点的 .theme-dark 覆盖） ========== */
page {
  --page-bg: #f5f6f8;
  --card-bg: #ffffff;
  --block-bg: #f7f8fa;
  --mute-bg: #f2f3f5;
  --text-primary: #1f2329;
  --text-secondary: #646a73;
  --text-tertiary: #8a8f99;
  --text-placeholder: #c0c4cc;
  --text-disabled: #a9aeb8;
  --border-color: #f0f1f3;
  --border-strong: #e5e6eb;
  --brand: #07c160;
  --brand-bg: #e8f8ef;
  --brand-disabled: #9fe0b8;
  --danger: #ff4d4f;
  --danger-bg: #ffeceb;
  --warn: #ff8f1f;
  --warn-bg: #fff4e6;

  /* 此处不设背景色：page 上的变量由其自身声明，.theme-dark 覆盖不到；
     页面底色由各页根节点铺满视口承担，窗口底色交给 pages.json + uni.setBackgroundColor；
     H5 下根元素会带上 theme-dark，暗色时 html 自身即铺满纯黑 */
  color: var(--text-primary);
  font-size: 28rpx;
}

/* 暗夜模式：页底纯黑 #000000，卡片深灰 #1a1a1a */
.theme-dark {
  --page-bg: #000000;
  --card-bg: #1a1a1a;
  --block-bg: #232323;
  --mute-bg: #232323;
  --text-primary: #f5f6f8;
  --text-secondary: #c0c4cc;
  --text-tertiary: #8a8f99;
  --text-placeholder: #8a8f99;
  --text-disabled: #6b7078;
  --border-color: #2a2a2a;
  --border-strong: #333333;
  --brand: #07c160;
  --brand-bg: #12351f;
  --brand-disabled: #2b5c3d;
  --danger: #ff6b6d;
  --danger-bg: #3a1f20;
  --warn: #ffa940;
  --warn-bg: #3a2c17;

  background-color: var(--page-bg);
  color: var(--text-primary);
}

/* ---------- H5 专用：导航栏纳入 CSS 主题层 ----------
   H5 的导航栏渲染在 uni-page-head 内，不属于页面根节点，.theme-dark 覆盖不到；
   且 uni.setNavigationBarColor 在 H5 只作用于页面栈末位，页面被复用时会写错对象。
   故 H5 改为在根元素（html）上打主题标记，由 CSS 直接驱动导航栏，随主题即时切换 */
/* #ifdef H5 */
html {
  --nav-bg: #ffffff;
  --nav-fg: #000000;
}

html.theme-dark {
  --nav-bg: #000000;
  --nav-fg: #ffffff;
}

/* !important 用于盖过 uni 写入的 inline style */
uni-page-head .uni-page-head {
  background-color: var(--nav-bg) !important;
  color: var(--nav-fg) !important;
}
/* #endif */

/* 通用卡片容器 */
.card {
  background-color: var(--card-bg);
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 20rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

/* 通用按钮 */
.btn {
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: none;
}

.btn::after {
  border: none;
}

.btn-primary {
  background-color: var(--brand);
  color: #ffffff;
}

.btn-primary[disabled] {
  background-color: var(--brand-disabled);
  color: #ffffff;
}

.btn-plain {
  background-color: var(--mute-bg);
  color: var(--text-primary);
}

/* 状态徽标（列表页 / 详情页共用，类名由 utils/voteState.js 的 voteStateClass 生成） */
.badge-draft,
.badge-notStarted,
.badge-ongoing,
.badge-expired,
.badge-stopped {
  font-size: 22rpx;
  line-height: 1;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
}

.badge-draft {
  color: var(--text-tertiary);
  background-color: var(--mute-bg);
}

.badge-notStarted {
  color: var(--warn);
  background-color: var(--warn-bg);
}

.badge-ongoing {
  color: var(--brand);
  background-color: var(--brand-bg);
}

.badge-expired {
  color: var(--danger);
  background-color: var(--danger-bg);
}

.badge-stopped {
  color: var(--text-tertiary);
  background-color: var(--mute-bg);
}
</style>
