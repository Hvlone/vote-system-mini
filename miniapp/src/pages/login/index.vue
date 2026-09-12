<template>
  <view class="login">
    <view class="brand">
      <view class="brand-title">线上投票</view>
      <view class="brand-sub">创建活动 · 分享参与 · 实时统计</view>
    </view>

    <view class="card">
      <view class="field">
        <text class="field-label">昵称（可空）</text>
        <input
          class="field-input"
          type="nickname"
          v-model="nickname"
          placeholder="填写后用于展示"
          placeholder-class="ph"
        />
      </view>

      <button class="btn btn-primary login-btn" :disabled="logining" @click="handleWxLogin">
        {{ logining ? '登录中...' : '微信一键登录' }}
      </button>

      <view class="debug">
        <view class="debug-title">调试登录（本地联调：code 以 test_ 开头）</view>
        <view class="debug-row">
          <input
            class="field-input debug-input"
            v-model="debugCode"
            placeholder="如 test_user1"
            placeholder-class="ph"
          />
          <button class="btn btn-plain debug-btn" :disabled="logining" @click="handleDebugLogin">
            调试登录
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login } from '../../api/auth'

const nickname = ref('')
const debugCode = ref('')
const logining = ref(false)
// 登录成功后的回跳地址（如从答题页跳来时带入），为空则进列表页
const redirect = ref('')

/** 保存登录态（token + 用户信息），供 request.js 与列表页使用 */
function saveLoginState(data) {
  uni.setStorageSync('token', data.token)
  uni.setStorageSync('userInfo', {
    userId: data.userId,
    openid: data.openid,
    role: data.role,
    nickname: data.nickname
  })
}

/** 解析回跳地址：仅允许站内页面路径，兼容已解码 / 未解码两种入参 */
function normalizeRedirect(value) {
  if (!value) return ''
  let text = String(value)
  try {
    if (text.indexOf('%') > -1) text = decodeURIComponent(text)
  } catch (e) {
    return ''
  }
  return text.indexOf('/pages/') === 0 ? text : ''
}

/** 登录成功后的落地页：带 redirect 则回跳（如答题页），否则进列表页 */
function goNext() {
  if (redirect.value) {
    uni.redirectTo({ url: redirect.value })
  } else {
    uni.reLaunch({ url: '/pages/list/index' })
  }
}

/** 统一登录流程：拿 code 换 token */
async function doLogin(code) {
  if (logining.value) return
  logining.value = true
  try {
    const data = await login({ code, nickname: nickname.value.trim() || undefined })
    saveLoginState(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(goNext, 300)
  } catch (e) {
    // 失败提示已由 request.js 统一弹出
  } finally {
    logining.value = false
  }
}

/** 微信一键登录：wx.login 拿 code */
function handleWxLogin() {
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (!res.code) {
        uni.showToast({ title: '获取微信 code 失败', icon: 'none' })
        return
      }
      doLogin(res.code)
    },
    fail: () => {
      uni.showToast({ title: '当前环境不支持微信登录，请使用调试登录', icon: 'none' })
    }
  })
}

/** 调试登录：直接使用 test_ 开头的 code（后端不调微信，取后段当 openid） */
function handleDebugLogin() {
  const code = debugCode.value.trim()
  if (!code) {
    uni.showToast({ title: '请输入调试 code', icon: 'none' })
    return
  }
  doLogin(code)
}

onLoad((query) => {
  redirect.value = normalizeRedirect(query && query.redirect)
  // 已有 token 直接进入落地页，避免重复登录
  if (uni.getStorageSync('token')) {
    goNext()
  }
})
</script>

<style scoped>
.login {
  min-height: 100vh;
  padding-top: 120rpx;
}

.brand {
  padding: 0 48rpx 48rpx;
}

.brand-title {
  font-size: 52rpx;
  font-weight: bold;
  color: #1f2329;
}

.brand-sub {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #8a8f99;
}

.field {
  margin-bottom: 32rpx;
}

.field-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  color: #8a8f99;
}

.field-input {
  height: 84rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  background-color: #f7f8fa;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.ph {
  color: #c0c4cc;
}

.login-btn {
  margin-top: 8rpx;
}

.debug {
  margin-top: 48rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid #f0f1f3;
}

.debug-title {
  margin-bottom: 16rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.debug-row {
  display: flex;
  align-items: center;
}

.debug-input {
  flex: 1;
}

.debug-btn {
  width: 200rpx;
  margin-left: 20rpx;
  font-size: 26rpx;
}
</style>
