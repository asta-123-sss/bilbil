// =====================================================================
// 批量上传 6 个分区的演示视频（走网站真实上传接口）
// 用法：node scripts/upload_demo_videos.mjs
// 说明：境外样例视频源在当前网络下约 100KB/s，下载太慢，故改用本地已有的 mp4
//       （upload/20261009/*.mp4），按分区循环复用（内容会重复，但分区/标题不同）。
// 仅用 Node 内置模块（fetch/crypto/fs），无需 npm 安装。
// =====================================================================
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const BASE = 'http://47.243.8.223:8081'
const CHUNK_SIZE = 5 * 1024 * 1024

const CATEGORIES = [
  { id: 1, name: '游戏' },
  { id: 2, name: '生活' },
  { id: 3, name: '科技' },
  { id: 4, name: '动画' },
  { id: 5, name: '音乐' },
  { id: 6, name: '知识' },
]

const log = (...a) => console.log(...a)

// 统一请求：返回 { httpStatus, data }（data 为已解析的 JSON）
async function api(path, { method = 'GET', token, json, rawBody } = {}) {
  const headers = {}
  if (token) headers.token = token
  let body
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
  } else if (rawBody) {
    headers['Content-Type'] = rawBody.contentType
    body = rawBody.buffer
  }
  const res = await fetch(BASE + path, { method, headers, body })
  const text = await res.text()
  let data
  try { data = JSON.parse(text) } catch { data = text }
  return { httpStatus: res.status, data }
}

// 解析 Result{code,msg,data} 包，非 200 抛异常
function unwrap(resp, ctx) {
  const d = resp.data
  const ok = resp.httpStatus === 200 && d && (d.code === undefined || d.code === 200)
  if (!ok) throw new Error(`${ctx} 失败: HTTP ${resp.httpStatus} ${JSON.stringify(d)}`)
  return d.code === undefined ? d : d.data
}

// 找本地已有的 mp4 文件
function findLocalVideos() {
  // 只取本地完整 mp4（过滤掉 < 500KB 的截断文件），避免误传半截视频
  const dir = join(__dirname, '..', 'upload', '20261009')
  const files = []
  try {
    for (const f of readdirSync(dir)) {
      if (!f.toLowerCase().endsWith('.mp4')) continue
      const p = join(dir, f)
      if (statSync(p).size >= 500 * 1024) files.push(p)
    }
  } catch { /* 目录不存在则跳过 */ }
  return files
}

// 注册演示账号并登录拿 token
async function registerAndLogin() {
  const username = 'demo_upload_' + String(Date.now()).slice(-6)
  const password = 'demo123456'
  try {
    const r = await api('/api/user/register', {
      method: 'POST',
      json: { username, password, nickname: '演示UP主' },
    })
    if (r.data && r.data.code === 200) log(`注册成功: ${username}`)
    else log(`注册未成功(code=${r.data && r.data.code})，尝试直接登录...`)
  } catch (e) {
    log('注册异常: ' + e.message + '，尝试直接登录...')
  }
  const l = await api('/api/user/login', { method: 'POST', json: { username, password } })
  const token = unwrap(l, '登录')?.token
  if (!token) throw new Error('登录未返回 token: ' + JSON.stringify(l.data))
  log(`登录成功: ${username}  token=${token.slice(0, 12)}...`)
  return { username, token }
}

// 构造 multipart/form-data（字段 + 一个 file 文件）
function buildMultipart(boundary, fields, filename, fileBuf) {
  const parts = []
  for (const [name, value] of Object.entries(fields)) {
    parts.push(Buffer.from(
      `--${boundary}\r\nContent-Disposition: form-data; name="${name}"\r\n\r\n${value}\r\n`,
    ))
  }
  parts.push(Buffer.from(
    `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${filename}"\r\nContent-Type: application/octet-stream\r\n\r\n`,
  ))
  parts.push(fileBuf)
  parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
  return Buffer.concat(parts)
}

async function uploadOne(token, category, fileBuf, fileName) {
  const fileSize = fileBuf.length
  const chunkCount = Math.ceil(fileSize / CHUNK_SIZE)

  // 1. init（不传 fileMd5，避免与已上传文件秒传去重冲突）
  const init = await api('/api/video/upload/init', {
    method: 'POST',
    token,
    json: { fileName, fileSize, chunkSize: CHUNK_SIZE, chunkCount },
  })
  const uploadId = unwrap(init, 'init').uploadId

  // 2. 分块上传
  for (let i = 0; i < chunkCount; i++) {
    const chunk = fileBuf.subarray(i * CHUNK_SIZE, Math.min((i + 1) * CHUNK_SIZE, fileSize))
    const boundary = '----bilbil' + Date.now() + i
    const body = buildMultipart(boundary, { uploadId, chunkIndex: String(i) }, `chunk${i}`, chunk)
    const up = await api('/api/video/upload/chunk', {
      method: 'POST',
      token,
      rawBody: { contentType: `multipart/form-data; boundary=${boundary}`, buffer: body },
    })
    unwrap(up, `chunk${i}`)
  }

  // 3. merge（合并后 MQ 关闭时自动发布 status=1）
  const title = `【${category.name}】${category.name}演示视频 ${String(Date.now()).slice(-4)}`
  const merge = await api('/api/video/upload/merge', {
    method: 'POST',
    token,
    json: {
      uploadId,
      title,
      description: `自动生成的${category.name}分区演示视频`,
      categoryId: category.id,
      tags: `${category.name},演示`,
      coverUrl: null,
      visibility: 0,
      duration: 0,
    },
  })
  return unwrap(merge, 'merge')
}

async function main() {
  // 1. 取本地视频文件
  const localFiles = findLocalVideos()
  if (localFiles.length === 0) throw new Error('未找到本地 mp4 文件（upload/20261009 或 scripts/.tmp）')
  log(`本地视频文件：`)
  localFiles.forEach((f) => log(`  ${f}`))

  // 2. 注册 + 登录
  const { username, token } = await registerAndLogin()

  // 3. 逐个上传（文件不够时循环复用）
  log('')
  for (let i = 0; i < CATEGORIES.length; i++) {
    const cat = CATEGORIES[i]
    const filePath = localFiles[i % localFiles.length]
    const buf = readFileSync(filePath)
    const fileName = `demo_${cat.name}_v${i}.mp4`
    try {
      const videoId = await uploadOne(token, cat, buf, fileName)
      log(`✅ 上传成功 categoryId=${cat.id} ${cat.name} -> videoId=${videoId} (${(buf.length / 1024).toFixed(0)}KB)`)
    } catch (e) {
      log(`❌ 上传失败 categoryId=${cat.id} ${cat.name}: ${e.message}`)
    }
  }
  log(`\n完成。上传账号: ${username}`)
}

main().catch((e) => {
  console.error('脚本异常终止:', e)
  process.exit(1)
})
