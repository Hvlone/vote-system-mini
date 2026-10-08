<template>
  <view v-if="visible" class="dt-mask" @click="emit('close')" @touchmove.stop.prevent="blockScroll">
    <view class="dt-panel" @click.stop>
      <view class="dt-title">修改截止时间</view>

      <view class="dt-row">
        <text class="dt-label">日期</text>
        <picker mode="date" :value="date" :start="minDate" @change="onDateChange">
          <view class="dt-value" :class="{ 'dt-empty': !date }">
            {{ date || '选择日期' }}
          </view>
        </picker>
      </view>

      <view class="dt-row">
        <text class="dt-label">时间</text>
        <picker mode="time" :value="time" :start="minTime" @change="onTimeChange">
          <view class="dt-value" :class="{ 'dt-empty': !time }">
            {{ time || '选择时间' }}
          </view>
        </picker>
      </view>

      <view class="dt-tip">新截止时间必须晚于当前时间</view>

      <view class="dt-actions">
        <button class="btn btn-plain dt-btn" @click="emit('close')">取消</button>
        <button class="btn btn-primary dt-btn" @click="onConfirm">确定</button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 修改截止时间弹窗（#2）
 *
 * uni.showModal 只能承载标题+文本，塞不进 picker，故自绘遮罩弹窗。
 * 内部即创建页同款的「date + time 双 picker」写法，额外加遮罩与取消/确定。
 * 组件只负责"选时间"，合法性校验（Q6：必须晚于当前时间）由父组件负责。
 */
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** 初值 "yyyy-MM-dd HH:mm"（父组件用 formatDateTime 生成） */
  value: { type: String, default: '' }
})
const emit = defineEmits(['close', 'confirm'])

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

const date = ref('')
const time = ref('')

/** 最早可选日期 = 今天（Q6 前端下限锁为"现在"） */
const minDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
})

/** 所选日期为今天时，时间下限为当前 HH:mm，否则不限制 */
const minTime = computed(() => {
  if (date.value !== minDate.value) return '00:00'
  const d = new Date()
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
})

// 每次打开按传入值初始化；未传则回落到今天 23:59
watch(
  () => props.visible,
  (v) => {
    if (!v) return
    const [d, t] = String(props.value || '').split(' ')
    date.value = d || minDate.value
    time.value = t || '23:59'
  }
)

function onDateChange(e) {
  date.value = e.detail.value
}

function onTimeChange(e) {
  time.value = e.detail.value
}

/** 交回 "yyyy-MM-dd HH:mm"，由父组件转 ISO 并校验 */
function onConfirm() {
  if (!date.value || !time.value) return
  emit('confirm', `${date.value} ${time.value}`)
}

/** 仅用于阻止遮罩层下页面跟着滚动 */
function blockScroll() {}
</script>

<style scoped>
.dt-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
}

.dt-panel {
  width: 600rpx;
  padding: 40rpx 32rpx 24rpx;
  background-color: var(--card-bg);
  border-radius: 20rpx;
}

.dt-title {
  margin-bottom: 24rpx;
  font-size: 32rpx;
  font-weight: bold;
  text-align: center;
  color: var(--text-primary);
}

.dt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--border-color);
}

.dt-label {
  font-size: 28rpx;
  color: var(--text-secondary);
}

.dt-value {
  min-width: 240rpx;
  padding: 12rpx 20rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  background-color: var(--block-bg);
  border-radius: 12rpx;
  text-align: center;
}

.dt-empty {
  color: var(--text-placeholder);
}

.dt-tip {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.dt-actions {
  display: flex;
  margin-top: 32rpx;
}

.dt-btn {
  flex: 1;
  margin: 0 8rpx;
}
</style>
