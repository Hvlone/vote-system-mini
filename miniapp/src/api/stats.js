/**
 * 统计接口（仅创建者可见，非创建者 403）
 */
import { request } from '../utils/request'

/** 统计汇总：totalVotes + 每题选项计数与占比 */
export const getStats = (id) => request({ url: `/votes/${id}/stats` })

/**
 * 统计明细：固定每页 20 条且无 total，只能按"返回条数 < 20 即到底"判断加载更多
 * @param {Number} page 页码，从 1 开始
 */
export const getDetails = (id, page = 1) =>
  request({ url: `/votes/${id}/details?page=${page}` })
