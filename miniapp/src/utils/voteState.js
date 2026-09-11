/**
 * 投票展示态推导
 *
 * 后端只提供 status（0 草稿 / 1 发布中 / 2 已结束）与推导字段 expired，
 * "未开始"（status=1 且开始时间在未来）后端没有标志位，由前端推导。
 */

import { parseDateTime } from './datetime'

export const VOTE_STATE = {
  DRAFT: 'draft',
  NOT_STARTED: 'notStarted',
  ONGOING: 'ongoing',
  EXPIRED: 'expired',
  STOPPED: 'stopped'
}

/** 展示态 → 文案 */
export const VOTE_STATE_TEXT = {
  draft: '草稿',
  notStarted: '未开始',
  ongoing: '进行中',
  expired: '已截止',
  stopped: '已结束'
}

/**
 * 推导投票展示态
 * @param {Object} vote 后端返回的 Vote（含 status / expired / startTime）
 * @returns {String} VOTE_STATE 中的取值
 */
export function voteState(vote) {
  if (!vote) return VOTE_STATE.ONGOING
  if (vote.status === 0) return VOTE_STATE.DRAFT
  if (vote.status === 2) return VOTE_STATE.STOPPED
  if (vote.expired === true) return VOTE_STATE.EXPIRED

  const start = parseDateTime(vote.startTime)
  if (start && start.getTime() > Date.now()) return VOTE_STATE.NOT_STARTED
  return VOTE_STATE.ONGOING
}

/** 展示态 → 文案 */
export function voteStateText(vote) {
  return VOTE_STATE_TEXT[voteState(vote)] || ''
}

/** 展示态 → 徽标样式类名（样式定义在 App.vue 全局样式，列表页与详情页共用） */
export function voteStateClass(vote) {
  return 'badge-' + voteState(vote)
}

/**
 * 是否处于可填写时间窗（用于答题页禁用提交）
 * 后端提交闸门①要求 status=1 且 now ∈ [start_time, end_time]
 */
export function canSubmit(vote) {
  if (!vote || vote.status !== 1) return false
  const now = Date.now()
  const start = parseDateTime(vote.startTime)
  const end = parseDateTime(vote.endTime)
  if (start && start.getTime() > now) return false
  if (end && end.getTime() < now) return false
  return true
}
