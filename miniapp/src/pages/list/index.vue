<template>
  <view class="list">
    <view class="header">
      <view class="header-left">
        <text class="header-title">我创建的投票</text>
        <text class="header-sub">{{ nickname || '未设置昵称' }}</text>
      </view>
      <text class="header-action" @click="goMine">我的</text>
    </view>

    <view class="create-row">
      <button class="btn btn-primary" @click="goCreate">+ 创建投票</button>
    </view>

    <view v-if="loading" class="tip">加载中...</view>

    <view v-else-if="!votes.length" class="card empty">
      <text class="empty-title">还没有创建过投票</text>
      <text class="empty-sub">参与他人活动需通过分享或扫码进入</text>
    </view>

    <view v-else>
      <view v-for="item in votes" :key="item.id" class="card vote" @click="goDetail(item)">
        <view class="vote-head">
          <text class="vote-title">{{ item.title }}</text>
          <text :class="voteStateClass(item)">{{ voteStateText(item) }}</text>
        </view>

        <view v-if="item.description" class="vote-desc">{{ item.description }}</view>

        <view class="vote-time">
          {{ formatDateTime(item.startTime) }} ~ {{ formatDateTime(item.endTime) }}
        </view>

        <view class="vote-actions">
          <text
            v-if="voteState(item) === VOTE_STATE.DRAFT"
            class="act act-primary"
            @click.stop="goEdit(item)"
            >编辑</text
          >
          <text
            v-if="voteState(item) === VOTE_STATE.DRAFT"
            class="act act-primary"
            @click.stop="handlePublish(item)"
            >发布</text
          >
          <text v-if="item.status === 1" class="act" @click.stop="handleStop(item)">停止</text>
          <text
            v-if="item.status !== 0"
            class="act act-primary"
            @click.stop="goStats(item)"
            >查看统计</text
          >
          <text class="act act-danger" @click.stop="handleDelete(item)">删除</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { listMyVotes, publishVote, stopVote, deleteVote } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import { VOTE_STATE, voteState, voteStateText, voteStateClass } from '../../utils/voteState'

const votes = ref([])
const loading = ref(false)
const nickname = ref('')

/** 拉取"我创建的投票"（仅本人创建，按创建时间倒序） */
async function loadVotes() {
  loading.value = true
  try {
    const data = await listMyVotes()
    votes.value = data || []
  } catch (e) {
    votes.value = []
  } finally {
    loading.value = false
  }
}

function goCreate() {
  uni.navigateTo({ url: '/pages/create/index' })
}

function goEdit(item) {
  uni.navigateTo({ url: `/pages/create/index?id=${item.id}` })
}

/** 管理视角详情：可读草稿，带编辑/发布/停止操作 */
function goDetail(item) {
  uni.navigateTo({ url: `/pages/detail/index?id=${item.id}&mode=manage` })
}

/** 查看统计：仅创建者可见（草稿无统计入口） */
function goStats(item) {
  uni.navigateTo({ url: `/pages/stats/index?id=${item.id}` })
}

/** 进入我的页（昵称/我的记录/退出登录都在那里） */
function goMine() {
  uni.navigateTo({ url: '/pages/mine/index' })
}

function handlePublish(item) {
  uni.showModal({
    title: '发布投票',
    content: `确定发布「${item.title}」？发布后不可再编辑。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await publishVote(item.id)
        uni.showToast({ title: '已发布', icon: 'success' })
        loadVotes()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

function handleStop(item) {
  uni.showModal({
    title: '停止投票',
    content: `确定停止「${item.title}」？停止后用户将无法继续提交。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await stopVote(item.id)
        uni.showToast({ title: '已停止', icon: 'success' })
        loadVotes()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

function handleDelete(item) {
  uni.showModal({
    title: '删除投票',
    content: `确定删除「${item.title}」？题目、选项与已提交记录将一并删除且不可恢复。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteVote(item.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        loadVotes()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

// 用 onShow 而非 onLoad：从创建/编辑页返回时需要自动刷新列表
onShow(() => {
  const info = uni.getStorageSync('userInfo')
  nickname.value = (info && info.nickname) || ''
  loadVotes()
})

onPullDownRefresh(async () => {
  await loadVotes()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.list {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 8rpx;
}

.header-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #1f2329;
}

.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.header-action {
  font-size: 26rpx;
  color: #8a8f99;
}

.create-row {
  padding: 24rpx 24rpx 4rpx;
}

.tip {
  padding: 80rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #8a8f99;
}

.empty {
  margin-top: 80rpx;
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

.vote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.vote-title {
  flex: 1;
  margin-right: 16rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #1f2329;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.vote-desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #646a73;
}

.vote-time {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.vote-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f0f1f3;
}

.act {
  margin-left: 32rpx;
  font-size: 26rpx;
  color: #646a73;
}

.act-primary {
  color: #07c160;
}

.act-danger {
  color: #ff4d4f;
}
</style>
