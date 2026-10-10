// 下架指定视频（走 DELETE /api/video/{id}，软下架 status=2）
// 用法：node scripts/offline_videos.mjs <videoId> [videoId...]
import { fileURLToPath } from 'node:url'

const BASE = 'http://47.243.8.223:8081'
const USERNAME = process.env.BILBIL_USER || 'demo_upload_055258'
const PASSWORD = process.env.BILBIL_PASS || 'demo123456'
const ids = process.argv.slice(2).map(Number).filter((n) => Number.isInteger(n) && n > 0)

async function main() {
  const login = await fetch(`${BASE}/api/user/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
  }).then((r) => r.json())
  const token = login && login.data && login.data.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(login))

  for (const id of ids) {
    const r = await fetch(`${BASE}/api/video/${id}`, { method: 'DELETE', headers: { token } }).then((x) => x.json())
    console.log(`下架 videoId=${id}: ${r && r.code === 200 ? 'OK' : JSON.stringify(r)}`)
  }
}

main().catch((e) => {
  console.error('异常:', e)
  process.exit(1)
})
