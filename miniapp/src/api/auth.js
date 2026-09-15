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

// 无 logout 接口：JWT 无状态，后端无法作废 token，退出登录由页面清本地 storage 完成
