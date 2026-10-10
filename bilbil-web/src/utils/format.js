// 播放量 / 点赞数等：过万显示为 x.x万
export function formatCount(n) {
  n = Number(n) || 0
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

// 秒数 -> mm:ss
export function formatDuration(s) {
  s = Math.floor(Number(s) || 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

// 时间戳 -> 相对时间
export function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = Date.now()
  const diff = now - d.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 30 * day) return Math.floor(diff / day) + '天前'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
