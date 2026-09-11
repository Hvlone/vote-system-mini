/**
 * 时间工具
 *
 * 后端 LocalDateTime 无 @JsonFormat，时间字段需传 "yyyy-MM-dd'T'HH:mm:ss"；
 * 而 picker 组件输出的是 "yyyy-MM-dd HH:mm"，因此需要互转。
 * 注意：不使用 new Date(字符串) 直接解析，iOS 与部分小程序对 "yyyy-MM-dd HH:mm:ss" 解析不稳定，统一手动解析。
 */

const ISO_RE = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

/**
 * 把 picker 输出转成后端要求的 ISO 字符串
 * @param {String} text "yyyy-MM-dd HH:mm" 或 "yyyy-MM-ddTHH:mm"
 * @returns {String|null} "yyyy-MM-ddTHH:mm:ss"；入参为空时返回 null
 */
export function toIso(text) {
  if (!text) return null
  const s = String(text).trim().replace(' ', 'T')
  if (!s) return null
  return s.length === 16 ? s + ':00' : s
}

/**
 * 把后端时间字符串解析成本地 Date（手动解析，兼容 "yyyy-MM-ddTHH:mm:ss" 与 "yyyy-MM-dd HH:mm:ss"）
 * @returns {Date|null}
 */
export function parseDateTime(value) {
  if (!value) return null
  const m = ISO_RE.exec(String(value))
  if (!m) return null
  return new Date(
    Number(m[1]),
    Number(m[2]) - 1,
    Number(m[3]),
    Number(m[4]),
    Number(m[5]),
    Number(m[6] || 0)
  )
}

/**
 * 格式化为展示文案 "yyyy-MM-dd HH:mm"
 * @returns {String} 解析失败时返回空串
 */
export function formatDateTime(value) {
  const d = parseDateTime(value)
  if (!d) return ''
  return (
    d.getFullYear() +
    '-' +
    pad(d.getMonth() + 1) +
    '-' +
    pad(d.getDate()) +
    ' ' +
    pad(d.getHours()) +
    ':' +
    pad(d.getMinutes())
  )
}
