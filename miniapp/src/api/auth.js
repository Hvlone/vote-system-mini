/**
 * 认证接口
 */
import { request } from '../utils/request'

/**
 * 登录
 * @param {Object} data { code(必填), nickname?, avatarUrl? }
 *        code 以 test_ 开头时后端不调微信，直接取后段当 openid（本地联调用）
 * @returns {Promise<{token,userId,openid,role,nickname}>}
 */
export const login = (data) =>
  request({ url: '/auth/login', method: 'POST', data, auth: false })

/**
 * 退出登录：后端无状态，仅清 ThreadLocal，前端必须自行清理 storage
 */
export const logout = () => request({ url: '/auth/logout', method: 'POST' })
