<template>
  <view class="stats">
    <view v-if="loading" class="tip">加载中...</view>

    <block v-else-if="stats">
      <view class="card summary">
        <text class="summary-title">{{ voteTitle || '投票统计' }}</text>
        <view class="summary-total">
          共 <text class="summary-num">{{ stats.totalVotes }}</text> 票
        </view>
      </view>

      <view v-if="!stats.totalVotes" class="card empty">
        <text class="empty-title">还没有人提交</text>
        <text class="empty-sub">把活动分享给参与者后，统计会随提交实时更新</text>
      </view>

      <block v-else>
        <view v-for="(q, qi) in stats.questions" :key="q.questionId" class="card">
          <view class="q-head">
            <text class="q-title">{{ qi + 1 }}. {{ q.questionText }}</text>
            <text class="q-type">{{ typeLabel(q.questionType) }}</text>
          </view>

          <!-- 单选 / 多选：各选项票数与占比（占比由后端算好，此处仅按百分比画条） -->
          <view v-if="q.questionType !== QUESTION_TYPE.TEXT" class="options">
            <view v-for="o in q.options" :key="o.optionId" class="opt">
              <view class="opt-head">
                <text class="opt-text">{{ o.optionText }}</text>
                <text class="opt-num">{{ o.count }} 票 · {{ o.percentage }}%</text>
              </view>
              <view class="bar">
                <view class="bar-inner" :style="{ width: (o.percentage || 0) + '%' }"></view>
              </view>
            </view>
          </view>

          <!-- 填空：只统计作答条数（具体文本在下方明细里看） -->
          <view v-else class="text-stat">已作答 {{ q.textAnswerCount || 0 }} 条</view>
        </view>

        <view class="card">
          <view class="section-head">
            <text class="section-title">提交明细</text>
            <text class="section-sub">已加载 {{ details.length }} 条</text>
          </view>

          <view v-if="!details.length" class="tip">暂无明细</view>

          <view v-for="(row, ri) in details" :key="row.id" class="record">
            <view class="record-head">
              <text class="record-index">#{{ ri + 1 }}</text>
              <text class="record-time">{{ formatDateTime(row.submittedAt) }}</text>
            </view>
            <view v-for="(a, ai) in row.answers" :key="ai" class="record-item">
              <text class="record-q">{{ a.label }}</text>
              <text class="record-a">{{ a.text || '（未作答）' }}</text>
            </view>
          </view>

          <view v-if="hasMore" class="more">
            <button class="btn btn-plain" :disabled="loadingMore" @click="loadMore">
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </button>
          </view>
          <view v-else-if="details.length" class="more-end">没有更多了</view>
        </view>
      </block>
    </block>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getStats, getDetails } from '../../api/stats'
import { getVoteDetail } from '../../api/vote'
import { formatDateTime } from '../../utils/datetime'

const QUESTION_TYPE = { SINGLE: 1, MULTI: 2, TEXT: 3 }
// 后端 /details 固定每页 20 条且无 total，只能以「返回条数 < 20」判定到底
const PAGE_SIZE = 20

const voteId = ref(null)
const voteTitle = ref('')
const stats = ref(null)
const details = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(false)
const page = ref(1)

// 明细翻译映射：统计接口已带全部题干与选项文案，无需再拉一次详情
let qTextMap = {}
let qTypeMap = {}
let oTextMap = {}

function typeLabel(type) {
  if (type === QUESTION_TYPE.SINGLE) return '单选'
  if (type === QUESTION_TYPE.MULTI) return '多选'
  return '填空'
}

function buildMaps() {
  qTextMap = {}
  qTypeMap = {}
  oTextMap = {}
  const list = (stats.value && stats.value.questions) || []
  list.forEach((q) => {
    qTextMap[q.questionId] = q.questionText
    qTypeMap[q.questionId] = q.questionType
    ;(q.options || []).forEach((o) => {
      oTextMap[o.optionId] = o.optionText
    })
  })
}

/** 把一条记录的 answerContent(JSON 字符串) 翻译成可读的「题干 → 答案」列表 */
function translate(content) {
  let parsed
  try {
    parsed = JSON.parse(content || '[]')
  } catch (e) {
    return []
  }
  if (!Array.isArray(parsed)) return []
  return parsed.map((a) => {
    const qid = a.questionId
    const answer = a.answer
    let text = ''
    if (qTypeMap[qid] === QUESTION_TYPE.TEXT) {
      text = answer === null || answer === undefined ? '' : String(answer)
    } else if (Array.isArray(answer)) {
      // 多选：逐个选项翻译后用顿号连接
      text = answer.map((id) => oTextMap[id] || '选项#' + id).join('、')
    } else if (answer !== null && answer !== undefined) {
      text = oTextMap[answer] || String(answer)
    }
    return { label: qTextMap[qid] || '题目#' + qid, text }
  })
}

async function loadDetails(reset) {
  const target = reset ? 1 : page.value
  loadingMore.value = true
  try {
    const rows = (await getDetails(voteId.value, target)) || []
    const mapped = rows.map((r) => ({ ...r, answers: translate(r.answerContent) }))
    details.value = reset ? mapped : details.value.concat(mapped)
    page.value = target + 1
    hasMore.value = rows.length === PAGE_SIZE
  } catch (e) {
    // 失败提示已由 request.js 弹出；置为到底避免重复请求
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadDetails(false)
}

async function loadStats() {
  loading.value = true
  try {
    // 统计接口不带活动标题，标题从管理视角详情取（同时兼作创建者权限校验）
    const [detail, data] = await Promise.all([
      getVoteDetail(voteId.value),
      getStats(voteId.value)
    ])
    voteTitle.value = (detail && detail.title) || ''
    stats.value = data || { totalVotes: 0, questions: [] }
    buildMaps()
    if (stats.value.totalVotes > 0) {
      await loadDetails(true)
    }
  } catch (e) {
    // 403（非创建者）/ 404（不存在）的提示已由 request.js 弹出
    setTimeout(() => uni.navigateBack(), 900)
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  voteId.value = query && query.id
  if (!voteId.value) {
    uni.showToast({ title: '缺少活动 id', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  loadStats()
})
</script>

<style scoped>
.stats {
  min-height: 100vh;
  padding-bottom: 60rpx;
}

.tip {
  padding: 40rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #8a8f99;
}

.summary-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #1f2329;
}

.summary-total {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #646a73;
}

.summary-num {
  font-size: 40rpx;
  font-weight: bold;
  color: #07c160;
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

.q-type {
  padding: 4rpx 12rpx;
  font-size: 22rpx;
  color: #8a8f99;
  background-color: #f2f3f5;
  border-radius: 8rpx;
}

.options {
  margin-top: 20rpx;
}

.opt {
  margin-bottom: 20rpx;
}

.opt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.opt-text {
  flex: 1;
  margin-right: 16rpx;
  font-size: 26rpx;
  color: #646a73;
}

.opt-num {
  font-size: 24rpx;
  color: #8a8f99;
}

.bar {
  height: 16rpx;
  margin-top: 10rpx;
  background-color: #f2f3f5;
  border-radius: 8rpx;
  overflow: hidden;
}

.bar-inner {
  height: 100%;
  background-color: #07c160;
  border-radius: 8rpx;
}

.text-stat {
  margin-top: 20rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: #646a73;
  background-color: #f7f8fa;
  border-radius: 12rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f1f3;
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

.record {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f7f8fa;
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-index {
  font-size: 24rpx;
  color: #07c160;
}

.record-time {
  font-size: 24rpx;
  color: #8a8f99;
}

.record-item {
  margin-top: 12rpx;
}

.record-q {
  display: block;
  font-size: 24rpx;
  color: #8a8f99;
}

.record-a {
  display: block;
  margin-top: 4rpx;
  font-size: 26rpx;
  color: #1f2329;
}

.more {
  margin-top: 24rpx;
}

.more-end {
  padding-top: 24rpx;
  text-align: center;
  font-size: 24rpx;
  color: #8a8f99;
}
</style>
