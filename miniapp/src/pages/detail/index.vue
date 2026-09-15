<template>
  <view class="detail" :class="{ 'theme-dark': theme.dark }">
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
        <!-- 管理视角（草稿）：编辑 / 发布 -->
        <block v-if="isManage && vote.status === 0">
          <button class="btn btn-plain footer-btn" @click="goEdit">编辑</button>
          <button class="btn btn-primary footer-btn" @click="handlePublish">发布</button>
        </block>

        <!-- 管理视角（发布中）：停止 -->
        <button
          v-else-if="isManage && vote.status === 1"
          class="btn btn-primary footer-btn"
          @click="handleStop"
        >
          停止投票
        </button>

        <!-- 管理视角（非草稿）：查看统计 -->
        <button
          v-if="isManage && vote.status !== 0"
          class="btn btn-plain footer-btn"
          @click="goStats"
        >
          查看统计
        </button>

        <!-- 参与入口：进行中且未投过 -->
        <button v-if="canParticipate" class="btn btn-primary footer-btn" @click="goAnswer">
          去投票
        </button>

        <!-- 不可参与：置灰文案（草稿由上面的管理按钮承担，不显示文案） -->
        <view v-else-if="disabledText" class="btn btn-disabled footer-btn">{{ disabledText }}</view>

        <!-- 转发分享入口：仅小程序端渲染（open-type=share 依赖微信环境）；草稿对外 404，不给分享入口 -->
        <!-- #ifdef MP-WEIXIN -->
        <button v-if="vote.status !== 0" class="btn btn-plain footer-btn" open-type="share">
          分享给好友
        </button>
        <!-- #endif -->
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import { getVoteDetail, getVisibleVote, publishVote, stopVote } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import {
  VOTE_STATE,
  voteState,
  voteStateText,
  voteStateClass,
  canSubmit
} from '../../utils/voteState'
import { useTheme } from '../../utils/theme'

const theme = useTheme()

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

/** 参与入口是否可用：发布中 + 在时间窗内 + 未投过 */
const canParticipate = computed(() => {
  if (!vote.value || vote.value.hasVoted === true) return false
  return canSubmit(vote.value)
})

/** 不可参与时的置灰文案（草稿由管理按钮承担，不显示文案） */
const disabledText = computed(() => {
  if (!vote.value) return ''
  if (vote.value.status === 0) return ''
  if (vote.value.hasVoted === true) return '已投票'
  const state = voteState(vote.value)
  if (state === VOTE_STATE.NOT_STARTED) return '未开始'
  if (state === VOTE_STATE.EXPIRED) return '已截止'
  if (state === VOTE_STATE.STOPPED) return '已结束'
  return ''
})

async function loadVote() {
  loading.value = true
  try {
    vote.value = isManage.value
      ? await getVoteDetail(voteId.value)
      : await getVisibleVote(voteId.value)
    // #ifdef MP-WEIXIN
    // 草稿对外一律 404，隐藏转发菜单避免分享出打不开的链接
    if (vote.value && vote.value.status === 0) {
      uni.hideShareMenu()
    }
    // #endif
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

/** 去答题 */
function goAnswer() {
  uni.navigateTo({ url: `/pages/answer/index?id=${voteId.value}` })
}

/** 查看统计：仅创建者可见（后端对非创建者返回 403） */
function goStats() {
  uni.navigateTo({ url: `/pages/stats/index?id=${voteId.value}` })
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

/** 登录守卫：投票详情需登录后才能浏览；未登录则跳登录页并带回跳地址 */
function ensureLogin() {
  if (uni.getStorageSync('token')) return true
  const back = `/pages/detail/index?id=${voteId.value}${isManage.value ? '&mode=manage' : ''}`
  uni.redirectTo({ url: `/pages/login/index?redirect=${encodeURIComponent(back)}` })
  return false
}

// 用 onShow：从编辑页返回时需要重新拉取最新内容；登录回跳后也走这里
onShow(() => {
  if (!voteId.value) {
    uni.showToast({ title: '缺少活动 id', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  if (!ensureLogin()) return
  loadVote()
})

/** 转发卡片标题：带前缀便于在会话列表里识别是投票活动 */
function shareTitle() {
  const title = vote.value && vote.value.title
  return title ? `【投票】${title}` : '邀请你参与投票'
}

// #ifdef MP-WEIXIN
/** 转发给好友：路径固定为分享视角（不带 mode=manage），对方点开即公开详情 */
onShareAppMessage(() => ({
  title: shareTitle(),
  path: `/pages/detail/index?id=${voteId.value}`
}))

/** 分享到朋友圈：朋友圈入口用 query（而非 path），页面路径由微信补全 */
onShareTimeline(() => ({
  title: shareTitle(),
  query: `id=${voteId.value}`
}))
// #endif
</script>

<style scoped>
.detail {
  min-height: 100vh;
  padding-bottom: 60rpx;
  background-color: var(--page-bg);
  /* 阻断首个子元素的上外边距逃逸出本容器，否则页面顶部会露出底层画布 */
  overflow: hidden;
}

.tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
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
  color: var(--text-primary);
}

.desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.time {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.voted-tip {
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: var(--brand);
  background-color: var(--brand-bg);
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
  color: var(--text-primary);
}

.q-required {
  padding: 4rpx 12rpx;
  font-size: 22rpx;
  color: var(--danger);
  background-color: var(--danger-bg);
  border-radius: 8rpx;
}

.q-type {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.options {
  margin-top: 16rpx;
}

.option {
  padding: 16rpx 20rpx;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  background-color: var(--block-bg);
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
  background-color: var(--border-strong);
  color: var(--text-disabled);
  text-align: center;
}
</style>
