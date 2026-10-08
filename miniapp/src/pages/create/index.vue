<template>
  <view class="create" :class="{ 'theme-dark': theme.dark }">
    <view class="card">
      <view class="field">
        <text class="label">投票标题 <text class="required">*</text></text>
        <input
          class="input"
          v-model="title"
          placeholder="请输入投票标题"
          placeholder-class="ph"
          maxlength="50"
        />
      </view>

      <view class="field">
        <text class="label">活动说明</text>
        <textarea
          class="textarea"
          v-model="description"
          placeholder="选填，向参与者说明活动背景"
          placeholder-class="ph"
          maxlength="200"
        />
      </view>
    </view>

    <view class="card">
      <view class="card-title">起止时间</view>
      <view class="tip">留空则默认[ 现在 ~ 7天后 ]</view>

      <view class="time-row">
        <text class="time-label">开始</text>
        <picker mode="date" :value="startDate" @change="onStartDateChange">
          <view class="time-value" :class="{ 'time-empty': !startDate }">
            {{ startDate || '选择日期' }}
          </view>
        </picker>
        <picker mode="time" :value="startClock" @change="onStartClockChange">
          <view class="time-value" :class="{ 'time-empty': !startClock }">
            {{ startClock || '选择时间' }}
          </view>
        </picker>
      </view>

      <view class="time-row">
        <text class="time-label">结束</text>
        <picker mode="date" :value="endDate" @change="onEndDateChange">
          <view class="time-value" :class="{ 'time-empty': !endDate }">
            {{ endDate || '选择日期' }}
          </view>
        </picker>
        <picker mode="time" :value="endClock" @change="onEndClockChange">
          <view class="time-value" :class="{ 'time-empty': !endClock }">
            {{ endClock || '选择时间' }}
          </view>
        </picker>
      </view>
    </view>

    <view v-for="(q, qi) in questions" :key="q._key" class="card">
      <view class="q-head">
        <text class="q-index">第 {{ qi + 1 }} 题</text>
        <view class="q-ops">
          <text v-if="qi > 0" class="q-op" @click="moveQuestion(qi, -1)">上移</text>
          <text v-if="qi < questions.length - 1" class="q-op" @click="moveQuestion(qi, 1)">
            下移
          </text>
          <text class="q-op q-op-danger" @click="removeQuestion(qi)">删除</text>
        </view>
      </view>

      <input
        class="input"
        v-model="q.questionText"
        placeholder="请输入题目内容"
        placeholder-class="ph"
        maxlength="100"
      />

      <view class="chips">
        <text
          v-for="(label, ti) in TYPE_LABELS"
          :key="label"
          class="chip"
          :class="{ 'chip-active': q.questionType === ti + 1 }"
          @click="setQuestionType(q, ti + 1)"
          >{{ label }}</text
        >
      </view>

      <view class="switch-row">
        <text class="label switch-label">必填</text>
        <switch :checked="q.isRequired === 1" color="#07c160" @change="onRequiredChange(q, $event)" />
      </view>

      <view v-if="q.questionType !== QUESTION_TYPE.TEXT" class="options">
        <view v-for="(o, oi) in q.options" :key="o._key" class="option-row">
          <text class="option-index">{{ oi + 1 }}</text>
          <input
            class="input option-input"
            v-model="o.optionText"
            placeholder="选项内容"
            placeholder-class="ph"
            maxlength="50"
          />
          <text class="q-op q-op-danger" @click="removeOption(q, oi)">删除</text>
        </view>
        <text class="add-option" @click="addOption(q)">+ 添加选项</text>
      </view>
      <view v-else class="tip">填空题由参与者自行输入文本，无需选项</view>
    </view>

    <view class="card">
      <button class="btn btn-plain" @click="addQuestion">+ 添加题目</button>
    </view>

    <view class="footer">
      <button class="btn btn-primary" :disabled="submitting" @click="handlePrimary">
        {{ submitting ? '处理中...' : isEdit ? '发布' : '创建并发布' }}
      </button>
      <button class="btn btn-plain footer-second" :disabled="submitting" @click="handleSecondary">
        {{ submitting ? '处理中...' : isEdit ? '保存修改' : '存为草稿' }}
      </button>
      <view class="footer-tip">
        {{ isEdit ? '保存后仍为草稿，发布后参与者才可填写' : '存为草稿后需在草稿箱发布，参与者才可填写' }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { createVote, editVote, publishVote, getVoteDetail } from '../../api/vote'
import { VOTE_STATUS } from '../../utils/voteState'
import { toIso, formatDateTime } from '../../utils/datetime'
import { useTheme } from '../../utils/theme'

const theme = useTheme()

// 题型：与后端约定一致（1 单选 / 2 多选 / 3 填空）
const QUESTION_TYPE = { SINGLE: 1, MULTI: 2, TEXT: 3 }
const TYPE_LABELS = ['单选', '多选', '填空']

// 列表项本地唯一 key（编辑是先删后插，题目/选项 id 不可复用，故不能用后端 id 做 key）
let keySeed = 0
function nextKey() {
  keySeed += 1
  return 'k' + keySeed
}

const voteId = ref(null)
const title = ref('')
const description = ref('')
const startDate = ref('')
const startClock = ref('')
const endDate = ref('')
const endClock = ref('')
const questions = ref([])
const submitting = ref(false)

/** 编辑模式：带 id 进入即编辑已有草稿（按钮文案与提交行为随之切换） */
const isEdit = computed(() => !!voteId.value)

/**
 * 表单快照：只取会影响后端的字段，并剔除本地 _key
 * （_key 由 nextKey() 每次重新生成，不剔除会恒判为"已修改"）
 */
function snapshot() {
  return JSON.stringify({
    title: title.value,
    description: description.value,
    startDate: startDate.value,
    startClock: startClock.value,
    endDate: endDate.value,
    endClock: endClock.value,
    questions: questions.value.map((q) => ({
      questionText: q.questionText,
      questionType: q.questionType,
      isRequired: q.isRequired,
      options: q.options.map((o) => o.optionText)
    }))
  })
}

// 进入时的基线快照：新建模式在 onLoad 采；编辑模式在 loadForEdit 回填完成后采（该方法是异步的）
const baseline = ref('')

/** 是否有未保存的修改：与基线逐字段比较，改回原样即视为未修改（比"是否触碰过"更准） */
const dirty = computed(() => !!baseline.value && snapshot() !== baseline.value)

// #ifdef MP-WEIXIN
// 离开确认（#10）：仅在确有修改时启用守卫，避免无修改时打扰。
// 注意 wx.enableAlertBeforeUnload 只拦得住导航栏返回键与安卓物理返回键，iOS 手势侧滑拦不住（见 03 文档 B-1）
let unloadGuardOn = false
watch(dirty, (changed) => {
  if (changed && !unloadGuardOn) {
    wx.enableAlertBeforeUnload({ message: '离开后修改将丢失，确定离开吗？' })
    unloadGuardOn = true
  } else if (!changed && unloadGuardOn) {
    wx.disableAlertBeforeUnload()
    unloadGuardOn = false
  }
})

// 页面卸载兜底：守卫若仍开着则解除，避免残留影响其它页面
onUnload(() => {
  releaseGuard()
})
// #endif

/** 程序化退出（保存/发布/失败返回）前必须先解除守卫，否则跳转会被拦下；非小程序端为空实现 */
function releaseGuard() {
  // #ifdef MP-WEIXIN
  if (unloadGuardOn) {
    wx.disableAlertBeforeUnload()
    unloadGuardOn = false
  }
  // #endif
}

/** 新建一个空题目 */
function newQuestion() {
  return {
    _key: nextKey(),
    questionText: '',
    questionType: QUESTION_TYPE.SINGLE,
    isRequired: 1,
    options: [
      { _key: nextKey(), optionText: '' },
      { _key: nextKey(), optionText: '' }
    ]
  }
}

/** picker 的日期与时间合成 "yyyy-MM-dd HH:mm"；只选日期时时间默认 00:00 */
function combine(date, clock) {
  if (!date) return ''
  return date + ' ' + (clock || '00:00')
}

function onStartDateChange(e) {
  startDate.value = e.detail.value
}
function onStartClockChange(e) {
  startClock.value = e.detail.value
}
function onEndDateChange(e) {
  endDate.value = e.detail.value
}
function onEndClockChange(e) {
  endClock.value = e.detail.value
}

function addQuestion() {
  questions.value.push(newQuestion())
}

function removeQuestion(index) {
  if (questions.value.length <= 1) {
    uni.showToast({ title: '至少保留一道题目', icon: 'none' })
    return
  }
  questions.value.splice(index, 1)
}

function moveQuestion(index, delta) {
  const arr = questions.value
  const target = index + delta
  if (target < 0 || target >= arr.length) return
  const removed = arr.splice(index, 1)[0]
  arr.splice(target, 0, removed)
}

/** 切换题型：由填空切回单选/多选且无选项时补两个空选项 */
function setQuestionType(question, type) {
  question.questionType = type
  if (type !== QUESTION_TYPE.TEXT && question.options.length === 0) {
    question.options.push({ _key: nextKey(), optionText: '' })
    question.options.push({ _key: nextKey(), optionText: '' })
  }
}

function onRequiredChange(question, e) {
  question.isRequired = e.detail.value ? 1 : 0
}

function addOption(question) {
  question.options.push({ _key: nextKey(), optionText: '' })
}

function removeOption(question, index) {
  if (question.options.length <= 1) {
    uni.showToast({ title: '至少保留一个选项', icon: 'none' })
    return
  }
  question.options.splice(index, 1)
}

/** 提交前本地校验，口径与后端一致，避免白跑一次请求 */
function validate() {
  if (!title.value.trim()) {
    uni.showToast({ title: '请输入投票标题', icon: 'none' })
    return false
  }
  if (!questions.value.length) {
    uni.showToast({ title: '至少添加一道题目', icon: 'none' })
    return false
  }
  for (let i = 0; i < questions.value.length; i++) {
    const q = questions.value[i]
    if (!q.questionText.trim()) {
      uni.showToast({ title: `第 ${i + 1} 题题干未填写`, icon: 'none' })
      return false
    }
    if (q.questionType === QUESTION_TYPE.TEXT) continue
    if (!q.options.length) {
      uni.showToast({ title: `第 ${i + 1} 题至少需要一个选项`, icon: 'none' })
      return false
    }
    for (let j = 0; j < q.options.length; j++) {
      if (!q.options[j].optionText.trim()) {
        uni.showToast({ title: `第 ${i + 1} 题第 ${j + 1} 个选项未填写`, icon: 'none' })
        return false
      }
    }
  }
  return true
}

/** 组装后端 VoteCreateDTO：sortOrder 按数组下标递增（后端按 sort_order 排序） */
function buildPayload() {
  return {
    title: title.value.trim(),
    description: description.value.trim() || null,
    startTime: toIso(combine(startDate.value, startClock.value)),
    endTime: toIso(combine(endDate.value, endClock.value)),
    questions: questions.value.map((q, qi) => ({
      questionText: q.questionText.trim(),
      questionType: q.questionType,
      isRequired: q.isRequired,
      sortOrder: qi,
      options:
        q.questionType === QUESTION_TYPE.TEXT
          ? []
          : q.options.map((o, oi) => ({ optionText: o.optionText.trim(), sortOrder: oi }))
    }))
  }
}

/** 存草稿 / 保存修改：单段请求（新建时后端返回 voteId） */
async function saveDraft() {
  const payload = buildPayload()
  if (voteId.value) {
    await editVote(voteId.value, payload)
    return voteId.value
  }
  return await createVote(payload)
}

/** 次按钮：存为草稿 / 保存修改 */
async function handleSecondary() {
  if (submitting.value || !validate()) return
  submitting.value = true
  try {
    await saveDraft()
    releaseGuard()
    uni.showToast({ title: isEdit.value ? '已保存' : '已存为草稿', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch (e) {
    // 失败提示已由 request.js 统一弹出
  } finally {
    submitting.value = false
  }
}

/**
 * 主按钮：创建并发布 / 发布
 * 两段请求：① 先落草稿（新建 POST / 编辑 PUT），② 再发布。
 * ② 失败时草稿已留（Q10），跳草稿箱让用户重试，不删除不回滚。
 */
async function handlePrimary() {
  if (submitting.value || !validate()) return
  submitting.value = true
  let draftId = voteId.value
  try {
    draftId = await saveDraft()
  } catch (e) {
    submitting.value = false
    return
  }
  try {
    await publishVote(draftId)
    releaseGuard()
    uni.showToast({ title: '已发布', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/list/index' }), 500)
  } catch (e) {
    releaseGuard()
    uni.showToast({ title: '发布失败，内容已存为草稿', icon: 'none' })
    setTimeout(() => uni.reLaunch({ url: '/pages/drafts/index' }), 1200)
  } finally {
    submitting.value = false
  }
}

/** 编辑模式：回填已有草稿（仅草稿可编辑，非草稿直接返回） */
async function loadForEdit(id) {
  try {
    const vo = await getVoteDetail(id)
    if (vo.status !== VOTE_STATUS.DRAFT) {
      // 此处基线尚未采集（dirty 恒为 false），不必解除离开守卫
      uni.showToast({ title: '仅草稿可编辑', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    title.value = vo.title || ''
    description.value = vo.description || ''
    const startText = formatDateTime(vo.startTime)
    startDate.value = startText ? startText.slice(0, 10) : ''
    startClock.value = startText ? startText.slice(11, 16) : ''
    const endText = formatDateTime(vo.endTime)
    endDate.value = endText ? endText.slice(0, 10) : ''
    endClock.value = endText ? endText.slice(11, 16) : ''

    questions.value = (vo.questions || []).map((q) => ({
      _key: nextKey(),
      questionText: q.questionText || '',
      questionType: q.questionType || QUESTION_TYPE.SINGLE,
      isRequired: q.isRequired === 0 ? 0 : 1,
      options: (q.options || []).map((o) => ({ _key: nextKey(), optionText: o.optionText || '' }))
    }))

    // 回填完毕后采集基线：此后用户的任何改动才会被判定为"未保存的修改"
    baseline.value = snapshot()
  } catch (e) {
    // 回填失败：基线同样未采集，dirty 恒为 false，无需解除守卫
    setTimeout(() => uni.navigateBack(), 800)
  }
}

onLoad((query) => {
  if (query && query.id) {
    voteId.value = query.id
    uni.setNavigationBarTitle({ title: '编辑投票' })
    loadForEdit(query.id)
  } else {
    uni.setNavigationBarTitle({ title: '创建投票' })
    questions.value.push(newQuestion())
    // 新建模式：初始空表单即基线（预置的 1 道空题不算"修改"）
    baseline.value = snapshot()
  }
})
</script>

<style scoped>
.create {
  min-height: 100vh;
  padding-bottom: 60rpx;
  background-color: var(--page-bg);
  /* 阻断首个子元素的上外边距逃逸出本容器，否则页面顶部会露出底层画布 */
  overflow: hidden;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.field {
  margin-bottom: 32rpx;
}

.field:last-child {
  margin-bottom: 0;
}

.label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.required {
  color: var(--danger);
}

.input {
  height: 84rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  background-color: var(--block-bg);
  border-radius: 12rpx;
  box-sizing: border-box;
}

.textarea {
  width: 100%;
  height: 140rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  background-color: var(--block-bg);
  border-radius: 12rpx;
  box-sizing: border-box;
}

.ph {
  color: var(--text-placeholder);
}

.tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.time-row {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
}

.time-label {
  width: 80rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.time-value {
  min-width: 200rpx;
  margin-right: 16rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  color: var(--text-primary);
  background-color: var(--block-bg);
  border-radius: 12rpx;
  text-align: center;
}

.time-empty {
  color: var(--text-placeholder);
}

.q-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.q-index {
  font-size: 28rpx;
  font-weight: bold;
  color: var(--text-primary);
}

.q-ops {
  display: flex;
  align-items: center;
}

.q-op {
  margin-left: 28rpx;
  font-size: 24rpx;
  color: var(--text-secondary);
}

.q-op-danger {
  color: var(--danger);
}

.chips {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
}

.chip {
  margin-right: 16rpx;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  background-color: var(--mute-bg);
  border-radius: 100rpx;
}

.chip-active {
  color: #ffffff;
  background-color: var(--brand);
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.switch-label {
  margin-bottom: 0;
}

.options {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid var(--border-color);
}

.option-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.option-index {
  width: 40rpx;
  font-size: 26rpx;
  color: var(--text-tertiary);
}

.option-input {
  flex: 1;
}

.add-option {
  display: inline-block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: var(--brand);
}

.footer {
  margin-top: 40rpx;
  padding: 0 24rpx;
}

.footer-second {
  margin-top: 16rpx;
}

.footer-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
  text-align: center;
}
</style>
