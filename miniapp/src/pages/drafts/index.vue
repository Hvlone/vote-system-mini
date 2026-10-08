<template>
  <view class="drafts" :class="{ 'theme-dark': theme.dark }">
    <view class="header">
      <text class="header-title">草稿箱</text>
      <text class="header-sub">草稿仅在发布后对参与者可见</text>
    </view>

    <view v-if="loading" class="tip">加载中...</view>

    <view v-else-if="!drafts.length" class="card empty">
      <text class="empty-title">草稿箱是空的</text>
      <text class="empty-sub">创建投票时选择「存为草稿」会保存在这里</text>
    </view>

    <view v-else>
      <view v-for="item in drafts" :key="item.id" class="card vote" @click="goDetail(item)">
        <view class="vote-head">
          <text class="vote-title">{{ item.title }}</text>
          <text :class="voteStateClass(item)">{{ voteStateText(item) }}</text>
        </view>

        <view v-if="item.description" class="vote-desc">{{ item.description }}</view>

        <view class="vote-time">
          {{ formatDateTime(item.startTime) }} ~ {{ formatDateTime(item.endTime) }}
        </view>

        <view class="vote-actions">
          <text class="act act-primary" @click.stop="goEdit(item)">编辑</text>
          <text class="act act-primary" @click.stop="handlePublish(item)">发布</text>
          <text class="act act-danger" @click.stop="handleDelete(item)">删除</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listMyVotes, publishVote, deleteVote } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import { VOTE_STATUS, voteStateText, voteStateClass } from '../../utils/voteState'
import { useTheme } from '../../utils/theme'

const theme = useTheme()
const drafts = ref([])
const loading = ref(false)

/** 拉取我创建的投票并前端过滤出草稿（后端无 status 过滤参数，故在数据层拆分） */
async function loadDrafts() {
  loading.value = true
  try {
    const data = await listMyVotes()
    drafts.value = (data || []).filter((v) => v.status === VOTE_STATUS.DRAFT)
  } catch (e) {
    drafts.value = []
  } finally {
    loading.value = false
  }
}

function goEdit(item) {
  uni.navigateTo({ url: `/pages/create/index?id=${item.id}` })
}

/** 管理视角详情：可读草稿，带编辑/发布操作 */
function goDetail(item) {
  uni.navigateTo({ url: `/pages/detail/index?id=${item.id}&mode=manage` })
}

function handlePublish(item) {
  uni.showModal({
    title: '发布投票',
    content: `确定发布「${item.title}」？发布后题目不可修改，仅可调整截止时间。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await publishVote(item.id)
        uni.showToast({ title: '已发布', icon: 'success' })
        loadDrafts()
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
        loadDrafts()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

// 用 onShow 而非 onLoad：从创建/编辑页返回时需要自动刷新草稿列表
onShow(() => {
  loadDrafts()
})
</script>

<style scoped>
.drafts {
  min-height: 100vh;
  padding-bottom: 40rpx;
  background-color: var(--page-bg);
}

.header {
  padding: 32rpx 32rpx 8rpx;
}

.header-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.tip {
  padding: 80rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--text-tertiary);
}

.empty {
  margin-top: 80rpx;
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
  color: var(--text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.vote-desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.vote-time {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.vote-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid var(--border-color);
}

.act {
  margin-left: 32rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.act-primary {
  color: var(--brand);
}

.act-danger {
  color: var(--danger);
}
</style>
