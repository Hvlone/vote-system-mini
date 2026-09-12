<template>
  <view class="answer">
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

      <view
        v-for="(q, qi) in vote.questions"
        :id="'q' + q.id"
        :key="q.id"
        class="card"
        :class="{ 'q-error': errorId === q.id }"
      >
        <view class="q-head">
          <text class="q-title">{{ qi + 1 }}. {{ q.questionText }}</text>
          <text v-if="q.isRequired === 1" class="q-required">必填</text>
        </view>

        <!-- 单选：answer = 选项 id -->
        <view v-if="q.questionType === QUESTION_TYPE.SINGLE" class="options">
          <view
            v-for="o in q.options"
            :key="o.id"
            class="option"
            :class="{ 'option-active': answers[q.id] === o.id }"
            @click="selectSingle(q, o)"
          >
            <view class="mark mark-radio" :class="{ 'mark-on': answers[q.id] === o.id }"></view>
            <text class="option-text">{{ o.optionText }}</text>
          </view>
        </view>

        <!-- 多选：answer = [选项 id, ...] -->
        <view v-else-if="q.questionType === QUESTION_TYPE.MULTI" class="options">
          <view
            v-for="o in q.options"
            :key="o.id"
            class="option"
            :class="{ 'option-active': isMultiChecked(q.id, o.id) }"
            @click="toggleMulti(q, o)"
          >
            <view class="mark mark-check" :class="{ 'mark-on': isMultiChecked(q.id, o.id) }"></view>
            <text class="option-text">{{ o.optionText }}</text>
          </view>
        </view>

        <!-- 填空：answer = 文本 -->
        <textarea
          v-else
          class="q-textarea"
          :value="answers[q.id] || ''"
          :disabled="locked"
          maxlength="500"
          placeholder="请输入你的回答"
          placeholder-class="ph"
          @input="onTextInput(q, $event)"
        />
      </view>

      <view class="footer">
        <view v-if="locked" class="btn btn-disabled footer-btn">{{ lockedText }}</view>
        <button
          v-else
          class="btn btn-primary footer-btn"
          :disabled="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交' }}
        </button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getVisibleVote, submitVote } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'
import {
  VOTE_STATE,
  voteState,
  voteStateText,
  voteStateClass,
  canSubmit
} from '../../utils/voteState'

const QUESTION_TYPE = { SINGLE: 1, MULTI: 2, TEXT: 3 }

const voteId = ref(null)
const vote = ref(null)
const loading = ref(true)
const submitting = ref(false)
// 以 questionId 为键：单选存选项 id、多选存选项 id 数组、填空存文本
const answers = ref({})
// 必填校验未通过的题目 id，用于高亮定位
const errorId = ref(null)

/** 页面锁定：已投过，或不在可填写时间窗（未开始 / 已截止 / 已结束） */
const locked = computed(() => {
  if (!vote.value) return false
  if (vote.value.hasVoted === true) return true
  return !canSubmit(vote.value)
})

const lockedText = computed(() => {
  if (!vote.value) return ''
  if (vote.value.hasVoted === true) return '已投票'
  const state = voteState(vote.value)
  if (state === VOTE_STATE.NOT_STARTED) return '投票未开始'
  if (state === VOTE_STATE.EXPIRED) return '投票已截止'
  if (state === VOTE_STATE.STOPPED) return '投票已结束'
  return '当前不可提交'
})

async function loadVote() {
  loading.value = true
  try {
    vote.value = await getVisibleVote(voteId.value)
    resetAnswers()
  } catch (e) {
    // 404（不存在或草稿对外不可见）等失败提示已由 request.js 弹出
    setTimeout(() => uni.navigateBack(), 900)
  } finally {
    loading.value = false
  }
}

/** 初始化答案容器：多选为空数组便于勾选，其余为空串 */
function resetAnswers() {
  const next = {}
  const list = (vote.value && vote.value.questions) || []
  list.forEach((q) => {
    next[q.id] = q.questionType === QUESTION_TYPE.MULTI ? [] : ''
  })
  answers.value = next
}

function selectSingle(q, o) {
  if (locked.value) return
  answers.value[q.id] = o.id
  if (errorId.value === q.id) errorId.value = null
}

function isMultiChecked(qid, oid) {
  const arr = answers.value[qid]
  return Array.isArray(arr) && arr.indexOf(oid) > -1
}

function toggleMulti(q, o) {
  if (locked.value) return
  const arr = answers.value[q.id]
  const idx = arr.indexOf(o.id)
  if (idx > -1) {
    arr.splice(idx, 1)
  } else {
    arr.push(o.id)
  }
  if (errorId.value === q.id && arr.length) errorId.value = null
}

function onTextInput(q, e) {
  const text = (e && e.detail && e.detail.value) || ''
  answers.value[q.id] = text
  if (errorId.value === q.id && text.trim()) errorId.value = null
}

/** 必填校验：返回第一条未作答的必填题，全部通过返回 null */
function findFirstMissing() {
  const list = (vote.value && vote.value.questions) || []
  for (const q of list) {
    if (q.isRequired !== 1) continue
    const a = answers.value[q.id]
    if (q.questionType === QUESTION_TYPE.MULTI) {
      if (!Array.isArray(a) || !a.length) return q
    } else if (q.questionType === QUESTION_TYPE.TEXT) {
      if (!a || !String(a).trim()) return q
    } else if (a === '' || a === null || a === undefined) {
      return q
    }
  }
  return null
}

/** 组装提交体：只提交已作答的题目（后端只校验必填题），键名固定 questionId */
function buildAnswers() {
  const result = []
  const list = (vote.value && vote.value.questions) || []
  list.forEach((q) => {
    const a = answers.value[q.id]
    if (q.questionType === QUESTION_TYPE.MULTI) {
      if (Array.isArray(a) && a.length) result.push({ questionId: q.id, answer: a.slice() })
    } else if (q.questionType === QUESTION_TYPE.TEXT) {
      const text = a ? String(a).trim() : ''
      if (text) result.push({ questionId: q.id, answer: text })
    } else if (a !== '' && a !== null && a !== undefined) {
      result.push({ questionId: q.id, answer: a })
    }
  })
  return result
}

function scrollToQuestion(qid) {
  try {
    uni.pageScrollTo({ selector: '#q' + qid, duration: 300 })
  } catch (e) {
    // 个别平台不支持 selector 定位，忽略即可（高亮仍可见）
  }
}

/** 登录守卫：答题需登录后才能进入；未登录则跳登录页并带回跳地址 */
function ensureLogin() {
  if (uni.getStorageSync('token')) return true
  const back = `/pages/answer/index?id=${voteId.value}`
  uni.redirectTo({ url: `/pages/login/index?redirect=${encodeURIComponent(back)}` })
  return false
}

function handleSubmit() {
  if (submitting.value) return

  const missing = findFirstMissing()
  if (missing) {
    errorId.value = missing.id
    scrollToQuestion(missing.id)
    uni.showToast({ title: '必填题未作答：' + missing.questionText, icon: 'none' })
    return
  }

  uni.showModal({
    title: '提交投票',
    content: '提交后不可修改，确定提交吗？',
    success: (res) => {
      if (res.confirm) doSubmit()
    }
  })
}

async function doSubmit() {
  submitting.value = true
  try {
    await submitVote(voteId.value, buildAnswers())
    uni.showToast({ title: '提交成功', icon: 'success' })
    vote.value.hasVoted = true
    errorId.value = null
  } catch (e) {
    // 400 / 429 的提示已由 request.js 统一弹出，停留本页即可
    // 409：已投过（含唯一索引兜底）→ 直接锁定页面
    if (e && e.code === 409) vote.value.hasVoted = true
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  voteId.value = query && query.id
})

// 用 onShow：登录回跳或从其他页返回时，需要重新拉取最新的 hasVoted 与状态
onShow(() => {
  if (!voteId.value) {
    uni.showToast({ title: '缺少活动 id', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  if (!ensureLogin()) return
  loadVote()
})
</script>

<style scoped>
.answer {
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

.q-error {
  border: 2rpx solid #ff4d4f;
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

.options {
  margin-top: 16rpx;
}

.option {
  display: flex;
  align-items: center;
  padding: 20rpx;
  margin-bottom: 12rpx;
  font-size: 28rpx;
  color: #646a73;
  background-color: #f7f8fa;
  border-radius: 12rpx;
}

.option-active {
  color: #07c160;
  background-color: #e8f8ef;
}

.option-text {
  flex: 1;
}

.mark {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
  box-sizing: border-box;
  border: 2rpx solid #c0c4cc;
  background-color: #ffffff;
}

.mark-radio {
  border-radius: 50%;
}

.mark-check {
  border-radius: 6rpx;
}

.mark-on {
  border-color: #07c160;
  background-color: #07c160;
}

.q-textarea {
  width: 100%;
  height: 160rpx;
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  color: #1f2329;
  background-color: #f7f8fa;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.ph {
  color: #c0c4cc;
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
