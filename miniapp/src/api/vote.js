/**
 * 投票活动接口
 */
import { request } from '../utils/request'

/** 我创建的投票列表（仅本人创建，按创建时间倒序） */
export const listMyVotes = () => request({ url: '/votes/list' })

/** 公开详情（免登录可读，草稿一律 404；带 token 才回填 hasVoted） */
export const getVisibleVote = (id) => request({ url: `/votes/${id}/visible` })

/** 管理视角详情（草稿可读，非创建者 403） */
export const getVoteDetail = (id) => request({ url: `/votes/${id}` })

/** 创建投票（创建即草稿 status=0，返回 voteId；时间留空则后端补 now ~ +7d） */
export const createVote = (data) => request({ url: '/votes', method: 'POST', data })

/** 编辑投票（仅创建者 + 仅草稿；题目先删后插，题目/选项 id 不可复用） */
export const editVote = (id, data) =>
  request({ url: `/votes/${id}`, method: 'PUT', data })

/** 发布（status 0 → 1） */
export const publishVote = (id) =>
  request({ url: `/votes/${id}/publish`, method: 'POST' })

/** 停止（status 1 → 2） */
export const stopVote = (id) => request({ url: `/votes/${id}/stop`, method: 'POST' })

/** 删除（级联删题目 / 选项 / 投票记录） */
export const deleteVote = (id) => request({ url: `/votes/${id}`, method: 'DELETE' })

/**
 * 提交答案
 * @param {Array} answers [{ questionId, answer }]
 *        单选 answer = 选项id；多选 answer = [选项id,...]；填空 answer = "文本"
 */
export const submitVote = (id, answers) =>
  request({ url: `/votes/${id}/submit`, method: 'POST', data: { answers } })

/** 我参与过的投票记录（含 voteTitle，无分页） */
export const getMyRecords = () => request({ url: '/my/records' })
