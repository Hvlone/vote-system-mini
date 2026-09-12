<template>
  <view class="mine">
    <view class="card profile">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-info">
        <text class="profile-name">{{ nickname || '未设置昵称' }}</text>
        <text class="profile-sub">微信登录</text>
      </view>
    </view>

    <view class="create-row">
      <button class="btn btn-primary" @click="goCreate">+ 创建投票</button>
    </view>

    <view class="section-head">
      <text class="section-title">我参与的投票</text>
      <text v-if="records.length" class="section-sub">共 {{ records.length }} 条</text>
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
    </view>

    <view class="logout-row">
      <button class="btn btn-plain" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyRecords } from '../../api/vote'
import { logout } from '../../api/auth'
import { formatDateTime } from '../../utils/datetime'

const records = ref([])
const loading = ref(false)
const nickname = ref('')

// 后端无头像字段（登录仅回传 nickname），用昵称首字作默认头像
const avatarText = computed(() => (nickname.value ? nickname.value.charAt(0) : '我'))

/** 拉取「我参与过的投票」（含 voteTitle；后端无分页，一次性返回） */
async function loadRecords() {
  loading.value = true
  try {
    const data = await getMyRecords()
    records.value = data || []
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
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
    success: async (res) => {
      if (!res.confirm) return
      try {
        // 后端无状态，仅清 ThreadLocal；失败也要继续清本地登录态
        await logout()
      } catch (e) {
        // 忽略：本地登录态下面统一清理
      }
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({ url: '/pages/login/index' })
    }
  })
}

// 用 onShow：从创建页返回时需刷新「我参与的投票」（可能已新增记录）
onShow(() => {
  const info = uni.getStorageSync('userInfo')
  nickname.value = (info && info.nickname) || ''
  loadRecords()
})
</script>

<style scoped>
.mine {
  min-height: 100vh;
  padding-bottom: 60rpx;
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
  background-color: #07c160;
  border-radius: 50%;
}

.profile-info {
  flex: 1;
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: bold;
  color: #1f2329;
}

.profile-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a8f99;
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
  color: #1f2329;
}

.section-sub {
  font-size: 24rpx;
  color: #8a8f99;
}

.tip {
  padding: 60rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #8a8f99;
}

.empty {
  margin-top: 40rpx;
  padding: 64rpx 24rpx;
  text-align: center;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: #1f2329;
}

.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8a8f99;
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
  color: #1f2329;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.record-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
}

.record-time {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.logout-row {
  padding: 24rpx;
}
</style>
