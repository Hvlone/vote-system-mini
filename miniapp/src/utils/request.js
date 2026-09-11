/**
 * 统一请求封装（uni.request 的 Promise 化）
 *
 * 后端响应存在两条通道，必须同时判断，只看 HTTP 状态码或只看 body.code 都会漏：
 *   ① 未登录 / Token 失效：JwtInterceptor 直接写真实 HTTP 401
 *   ② 业务错误：GlobalExceptionHandler 返回 HTTP 200 + body.code（400/403/404/409/429/500）
 *   ③ 成功：HTTP 200 + body.code = 200
 */

// 接口基址：小程序开发期直连本机后端（需在开发者工具中勾选"不校验合法域名"）
const BASE_URL = 'http://localhost:8080/api/v1'

// 401 并发去重：多个请求同时失效时只跳转一次登录页
let redirectingToLogin = false

/** 清理本地登录态并跳回登录页 */
function toLogin() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  if (redirectingToLogin) return
  redirectingToLogin = true
  uni.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      redirectingToLogin = false
    }
  })
}

/**
 * 发起请求
 * @param {Object}  options
 * @param {String}  options.url     以 / 开头的接口路径（不含 BASE_URL）
 * @param {String}  [options.method] 请求方法，默认 GET
 * @param {Object}  [options.data]   请求体 / 查询参数
 * @param {Boolean} [options.auth]   是否携带 token，默认 true
 * @param {Boolean} [options.silent] 是否静默（不弹 toast），默认 false
 * @returns {Promise<any>} resolve 后端 body.data；reject { code, message }
 */
export function request(options = {}) {
  const { url, method = 'GET', data, auth = true, silent = false } = options

  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    const token = uni.getStorageSync('token')
    if (auth && token) {
      header.Authorization = 'Bearer ' + token
    }

    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success: (res) => {
        // 通道①：真实 HTTP 401
        if (res.statusCode === 401) {
          if (!silent) {
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          }
          toLogin()
          reject({ code: 401, message: '未登录或登录已过期' })
          return
        }

        // 通道②：业务错误（HTTP 200 + body.code）
        const body = res.data || {}
        if (body.code === 200) {
          resolve(body.data)
          return
        }
        const code = body.code === undefined ? res.statusCode : body.code
        const message = body.message || '请求失败'
        if (!silent) {
          uni.showToast({ title: message, icon: 'none' })
        }
        reject({ code, message })
      },
      fail: (err) => {
        if (!silent) {
          uni.showToast({ title: '网络异常，请检查后端服务是否已启动', icon: 'none' })
        }
        reject({ code: -1, message: '网络异常', raw: err })
      }
    })
  })
}

export { BASE_URL }
