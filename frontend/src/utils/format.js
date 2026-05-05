/** 金额保留2位小数 */
export function formatMoney(val) {
  if (val == null || val === '') return '0.00'
  return Number(val).toFixed(2)
}

/** ISO 时间字符串转 yyyy-MM-dd HH:mm */
export function formatTime(val, fallback = '-') {
  if (!val) return fallback
  const date = new Date(val)
  if (isNaN(date.getTime())) return fallback
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 分页总数转换为数字，兼容后端 Long 序列化为字符串 */
export function parseTotal(val) {
  const num = Number(val)
  return Number.isFinite(num) ? num : 0
}
