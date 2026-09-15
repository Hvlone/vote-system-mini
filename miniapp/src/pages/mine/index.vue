<template>
  <view class="mine" :class="{ 'theme-dark': theme.dark }">
    <view class="card profile">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-info">
        <text class="profile-name">{{ nickname || '未设置昵称' }}</text>
        <text class="profile-sub">微信登录</text>
      </view>
    </view>

    <view class="card setting">
      <view class="setting-row">
        <text class="setting-label">暗夜模式</text>
        <switch :checked="theme.dark" color="#07c160" @change="onThemeChange" />
      </view>
      <view class="setting-tip">默认开启；关闭后回到浅色界面</view>
    </view>

    <view class="create-row">
      <button class="btn btn-primary" @click="goCreate">+ 创建投票</button>
    </view>

    <view class="section-head">
      <text class="section-title">我参与的投票</text>
      <text v-if="total" class="section-sub">共 {{ total }} 条</text>
    </view>

    <view v-if="loading" class="tip">加载中...</view>

    <view v-else-if="!records.length" class="card empty">
      <text class="empty-title">还没有参与过投票</text>
      <text class="empty-sub">通过分享或扫码进入活动并提交后会显示在这里</text>
    </view>

    <view v-else>
      <view v-for="item in records" :key="item.id" class="card record" @click="goDetail(item)">
        <view class="record-head">
          <text class="record-title">{{ item.voteTitle || '投票活动' }}</text>
          <text class="record-arrow">›</text>
        </view>
        <view class="record-time">提交于 {{ formatDateTime(item.submittedAt) }}</view>
      </view>
      <view v-if="loadingMore" class="tip">加载中...</view>
      <view v-else-if="!hasMore" class="tip">没有更多了</view>
    </view>

    <view class="logout-row">
      <button class="btn btn-plain" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { getMyRecords } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import { setDark, useTheme } from '../../utils/theme'

const theme = useTheme()
const records = ref([])
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const page = ref(1)
const nickname = ref('')

/** 每页条数，与后端默认值一致（后台上限 50） */
const PAGE_SIZE = 20

// 后端无头像字段（登录仅回传 nickname），用昵称首字作默认头像
const avatarText = computed(() => (nickname.value ? nickname.value.charAt(0) : '我'))

/** 暗夜模式开关：立即生效并写入本地，下次启动沿用 */
function onThemeChange(e) {
  setDark(e.detail.value)
}

/**
 * 拉取「我参与过的投票」（含 voteTitle，后端分页）
 * @param {Boolean} reset true=回到第 1 页重新加载（onShow 刷新用）；false=追加下一页
 */
async function loadRecords(reset = true) {
  if (reset) {
    page.value = 1
    loading.value = true
  } else {
    // 正在加载或已无下一页时不再重复请求
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
  }
  try {
    const data = await getMyRecords(page.value, PAGE_SIZE)
    const list = (data && data.list) || []
    records.value = reset ? list : records.value.concat(list)
    total.value = (data && data.total) || 0
    hasMore.value = !!(data && data.hasMore)
    // 确认还有下一页才推进页码，避免请求异常后页码空跳导致漏数据
    if (hasMore.value) page.value += 1
  } catch (e) {
    if (reset) {
      records.value = []
      total.value = 0
      hasMore.value = false
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function goCreate() {
  uni.navigateTo({ url: '/pages/create/index' })
}

/** 记录点击进详情：以参与者视角浏览（已提交会显示已投票态） */
function goDetail(item) {
  uni.navigateTo({ url: `/pages/detail/index?id=${item.voteId}` })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定退出当前账号？',
    success: (res) => {
      if (!res.confirm) return
      // JWT 无状态、后端无 logout 接口，登出即清本地登录态
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({ url: '/pages/login/index' })
    }
  })
}

// 用 onShow：从创建页返回时需刷新「我参与的投票」（可能已新增记录），回到第 1 页重取
onShow(() => {
  const info = uni.getStorageSync('userInfo')
  nickname.value = (info && info.nickname) || ''
  loadRecords()
})

// 触底加载下一页（已无更多时 loadRecords 内部会直接返回）
onReachBottom(() => {
  loadRecords(false)
})
</script>

<style scoped>
.mine {
  min-height: 100vh;
  padding-bottom: 60rpx;
  background-color: var(--page-bg);
  /* 阻断首个子元素的上外边距逃逸出本容器，否则页面顶部会露出底层画布 */
  overflow: hidden;
}

.profile {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  line-height: 104rpx;
  margin-right: 24rpx;
  font-size: 40rpx;
  color: #ffffff;
  text-align: center;
  background-color: var(--brand);
  border-radius: 50%;
}

.profile-info {
  flex: 1;
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.profile-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.setting-label {
  font-size: 30rpx;
  color: var(--text-primary);
}

.setting-tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.create-row {
  padding: 8rpx 24rpx 4rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 8rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.section-sub {
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.tip {
  padding: 60rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--text-tertiary);
}

.empty {
  margin-top: 40rpx;
  padding: 64rpx 24rpx;
  text-align: center;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: var(--text-primary);
}

.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-title {
  flex: 1;
  margin-right: 16rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: var(--text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.record-arrow {
  font-size: 36rpx;
  color: var(--text-placeholder);
}

.record-time {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.logout-row {
  padding: 24rpx;
}
</style>
