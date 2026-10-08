<template>
  <view class="stats" :class="{ 'theme-dark': theme.dark }">
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
            <view class="section-right">
              <text class="section-sub">已加载 {{ details.length }} 条</text>
              <text class="export-link" @click="handleExport">
                {{ exporting ? '导出中...' : '导出' }}
              </text>
            </view>
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
import { useTheme } from '../../utils/theme'

const theme = useTheme()

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
const exporting = ref(false)

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
    return { qid, label: qTextMap[qid] || '题目#' + qid, text }
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

/** 拉取全量提交明细：/details 固定 20/页且无 total，只能以"返回条数 < 20"判定结束 */
async function fetchAllDetails() {
  const all = []
  for (let p = 1; ; p++) {
    const rows = (await getDetails(voteId.value, p)) || []
    rows.forEach((r) => all.push({ ...r, answers: translate(r.answerContent) }))
    if (rows.length < PAGE_SIZE) break
  }
  return all
}

/** CSV 单元格转义：含逗号/引号/换行时用双引号包裹，内部引号翻倍 */
function csvCell(value) {
  const text = value === null || value === undefined ? '' : String(value)
  return /[",\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text
}

/** 生成 CSV：表头按统计接口的题目顺序展开，每行按 questionId 对齐取值 */
function buildCsv(rows) {
  const questions = (stats.value && stats.value.questions) || []
  const header = ['序号', '提交时间'].concat(questions.map((q) => q.questionText))
  const lines = [header.map(csvCell).join(',')]
  rows.forEach((row, index) => {
    const byQid = {}
    ;(row.answers || []).forEach((a) => {
      byQid[a.qid] = a.text
    })
    const cells = [String(index + 1), formatDateTime(row.submittedAt)].concat(
      questions.map((q) => byQid[q.questionId] || '')
    )
    lines.push(cells.map(csvCell).join(','))
  })
  return lines.join('\r\n')
}

/** 导出（Q11）：纯前端拉全量明细 → 生成 CSV → 复制到剪贴板，用户自行粘贴到表格 */
async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  uni.showLoading({ title: '导出中...', mask: true })
  try {
    const rows = await fetchAllDetails()
    // 前置 BOM：让 Excel 按 UTF-8 解析，中文不乱码
    const csv = '\ufeff' + buildCsv(rows)
    uni.hideLoading()
    uni.setClipboardData({
      data: csv,
      success: () => {
        // setClipboardData 自带"已复制"轻提示，这里改用弹窗说明后续操作步骤
        uni.showModal({
          title: '导出完成',
          content: `已复制 ${rows.length} 条提交明细（CSV）。打开 Excel / 表格，在 A1 单元格粘贴即可。`,
          showCancel: false
        })
      }
    })
  } catch (e) {
    // 失败提示已由 request.js 统一弹出
  } finally {
    uni.hideLoading()
    exporting.value = false
  }
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
  background-color: var(--page-bg);
  /* 阻断首个子元素的上外边距逃逸出本容器，否则页面顶部会露出底层画布 */
  overflow: hidden;
}

.tip {
  padding: 40rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--text-tertiary);
}

.summary-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.summary-total {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.summary-num {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--brand);
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

.q-type {
  padding: 4rpx 12rpx;
  font-size: 22rpx;
  color: var(--text-tertiary);
  background-color: var(--mute-bg);
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
  color: var(--text-secondary);
}

.opt-num {
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.bar {
  height: 16rpx;
  margin-top: 10rpx;
  background-color: var(--mute-bg);
  border-radius: 8rpx;
  overflow: hidden;
}

.bar-inner {
  height: 100%;
  background-color: var(--brand);
  border-radius: 8rpx;
}

.text-stat {
  margin-top: 20rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  background-color: var(--block-bg);
  border-radius: 12rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid var(--border-color);
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

.section-right {
  display: flex;
  align-items: center;
}

.export-link {
  margin-left: 20rpx;
  padding: 4rpx 20rpx;
  font-size: 24rpx;
  color: var(--brand);
  border: 1rpx solid var(--brand);
  border-radius: 100rpx;
}

.record {
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--block-bg);
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-index {
  font-size: 24rpx;
  color: var(--brand);
}

.record-time {
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.record-item {
  margin-top: 12rpx;
}

.record-q {
  display: block;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.record-a {
  display: block;
  margin-top: 4rpx;
  font-size: 26rpx;
  color: var(--text-primary);
}

.more {
  margin-top: 24rpx;
}

.more-end {
  padding-top: 24rpx;
  text-align: center;
  font-size: 24rpx;
  color: var(--text-tertiary);
}
</style>
