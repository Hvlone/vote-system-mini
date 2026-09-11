<template>
  <view class="detail">
    <view v-if="loading" class="tip">加载中...</view>

    <block v-else-if="vote">
      <view class="card">
        <view class="head">
          <text class="title">{{ vote.title }}</text>
          <text :class="voteStateClass(vote)">{{ voteStateText(vote) }}</text>
        </view>
        <view v-if="vote.description" class="desc">{{ vote.description }}</view>
        <view class="time">
          {{ formatDateTime(vote.startTime) }} ~ {{ formatDateTime(vote.endTime) }}
        </view>
        <view v-if="vote.hasVoted === true" class="voted-tip">你已提交过该投票</view>
      </view>

      <view v-for="(q, qi) in vote.questions" :key="q.id" class="card">
        <view class="q-head">
          <text class="q-title">{{ qi + 1 }}. {{ q.questionText }}</text>
          <text class="q-required" v-if="q.isRequired === 1">必填</text>
        </view>
        <view class="q-type">{{ typeLabel(q.questionType) }}</view>

        <view v-if="q.questionType !== QUESTION_TYPE.TEXT" class="options">
          <view v-for="o in q.options" :key="o.id" class="option">{{ o.optionText }}</view>
        </view>
        <view v-else class="tip">填空题：由参与者自行填写文本</view>
      </view>

      <view class="footer">
        <block v-if="isManage">
          <button v-if="vote.status === 0" class="btn btn-plain footer-btn" @click="goEdit">
            编辑
          </button>
          <button v-if="vote.status === 0" class="btn btn-primary footer-btn" @click="handlePublish">
            发布
          </button>
          <button v-if="vote.status === 1" class="btn btn-primary footer-btn" @click="handleStop">
            停止投票
          </button>
        </block>

        <!-- 未开始 / 已截止 / 已结束 的禁用态提示；「去投票」入口在批 3 与答题页一起接入 -->
        <view v-if="vote.status !== 0 && voteState(vote) !== VOTE_STATE.ONGOING" class="btn btn-disabled footer-btn">
          {{ disabledText }}
        </view>
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getVoteDetail, getVisibleVote, publishVote, stopVote } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import {
  VOTE_STATE,
  voteState,
  voteStateText,
  voteStateClass
} from '../../utils/voteState'

const QUESTION_TYPE = { SINGLE: 1, MULTI: 2, TEXT: 3 }

const voteId = ref(null)
// manage：管理视角（从列表页进入，可读草稿）；public：分享 / 扫码视角（草稿不可见）
const isManage = ref(false)
const vote = ref(null)
const loading = ref(true)

function typeLabel(type) {
  if (type === QUESTION_TYPE.SINGLE) return '单选题'
  if (type === QUESTION_TYPE.MULTI) return '多选题'
  return '填空题'
}

const disabledText = computed(() => {
  if (!vote.value) return ''
  const state = voteState(vote.value)
  if (state === VOTE_STATE.NOT_STARTED) return '未开始'
  if (state === VOTE_STATE.EXPIRED) return '已截止'
  return '已结束'
})

async function loadVote() {
  loading.value = true
  try {
    vote.value = isManage.value
      ? await getVoteDetail(voteId.value)
      : await getVisibleVote(voteId.value)
  } catch (e) {
    // 404（不存在或草稿对外不可见）/ 403（非创建者）等失败提示已由 request.js 弹出
    setTimeout(() => uni.navigateBack(), 900)
  } finally {
    loading.value = false
  }
}

function goEdit() {
  uni.navigateTo({ url: `/pages/create/index?id=${voteId.value}` })
}

function handlePublish() {
  uni.showModal({
    title: '发布投票',
    content: `确定发布「${vote.value.title}」？发布后不可再编辑。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await publishVote(voteId.value)
        uni.showToast({ title: '已发布', icon: 'success' })
        loadVote()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

function handleStop() {
  uni.showModal({
    title: '停止投票',
    content: `确定停止「${vote.value.title}」？停止后用户将无法继续提交。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await stopVote(voteId.value)
        uni.showToast({ title: '已停止', icon: 'success' })
        loadVote()
      } catch (e) {
        // 失败提示已由 request.js 统一弹出
      }
    }
  })
}

onLoad((query) => {
  voteId.value = query && query.id
  isManage.value = !!(query && query.mode === 'manage')
})

// 用 onShow：从编辑页返回时需要重新拉取最新内容
onShow(() => {
  if (voteId.value) {
    loadVote()
  } else {
    uni.showToast({ title: '缺少活动 id', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  }
})
</script>

<style scoped>
.detail {
  min-height: 100vh;
  padding-bottom: 60rpx;
}

.tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #8a8f99;
  text-align: center;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  flex: 1;
  margin-right: 16rpx;
  font-size: 34rpx;
  font-weight: bold;
  color: #1f2329;
}

.desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #646a73;
}

.time {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.voted-tip {
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #07c160;
  background-color: #e8f8ef;
  border-radius: 8rpx;
}

.q-head {
  display: flex;
  align-items: center;
}

.q-title {
  flex: 1;
  margin-right: 16rpx;
  font-size: 30rpx;
  color: #1f2329;
}

.q-required {
  padding: 4rpx 12rpx;
  font-size: 22rpx;
  color: #ff4d4f;
  background-color: #ffeceb;
  border-radius: 8rpx;
}

.q-type {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.options {
  margin-top: 16rpx;
}

.option {
  padding: 16rpx 20rpx;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #646a73;
  background-color: #f7f8fa;
  border-radius: 12rpx;
}

.footer {
  display: flex;
  margin-top: 40rpx;
  padding: 0 24rpx;
}

.footer-btn {
  flex: 1;
  margin: 0 8rpx;
}

.btn-disabled {
  background-color: #e5e6eb;
  color: #a9aeb8;
  text-align: center;
}
</style>
