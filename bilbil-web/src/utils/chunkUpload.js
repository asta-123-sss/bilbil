import SparkMD5 from 'spark-md5'
import { initUpload, uploadChunk } from '../api/video'

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

/**
 * 增量计算整文件 MD5（分片读取，避免大文件一次性载入内存卡死主线程）
 */
export function computeFileMd5(file) {
  return new Promise((resolve, reject) => {
    const sliceSize = 2 * 1024 * 1024
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let offset = 0

    reader.onload = (e) => {
      spark.append(e.target.result)
      offset += sliceSize
      if (offset < file.size) {
        readNext()
      } else {
        resolve(spark.end())
      }
    }
    reader.onerror = (e) => reject(e)

    function readNext() {
      reader.readAsArrayBuffer(file.slice(offset, offset + sliceSize))
    }
    readNext()
  })
}

/** 简单的并发池：最多 limit 个任务并行执行 */
async function runWithConcurrency(tasks, limit) {
  const queue = [...tasks]
  const workerCount = Math.min(limit, queue.length)
  const workers = []
  for (let i = 0; i < workerCount; i++) {
    workers.push(
      (async () => {
        while (queue.length) {
          await queue.shift()()
        }
      })()
    )
  }
  await Promise.all(workers)
}

/**
 * 分块上传核心：秒传 + 断点续传 + 缺块并发上传
 * @param {File} file 待上传文件
 * @param {(uploaded:number, total:number)=>void} onProgress 进度回调
 * @returns {Promise<{uploadId:string, alreadyExists:boolean, videoId:number|null, chunkCount:number}>}
 */
export async function chunkUpload(file, onProgress) {
  const fileMd5 = await computeFileMd5(file)
  const chunkSize = CHUNK_SIZE
  const chunkCount = Math.ceil(file.size / chunkSize)

  // 1. 初始化上传会话（服务端返回已上传分片，支持断点续传 / 秒传）
  const initRes = await initUpload({
    fileName: file.name,
    fileSize: file.size,
    chunkSize,
    chunkCount,
    fileMd5
  })

  // 秒传：服务端已有相同 MD5 的文件
  if (initRes.alreadyExists) {
    onProgress && onProgress(chunkCount, chunkCount)
    return { uploadId: initRes.uploadId, alreadyExists: true, videoId: initRes.videoId, chunkCount }
  }

  const uploadId = initRes.uploadId
  const uploaded = new Set(initRes.uploadedIndexes || [])
  let done = uploaded.size
  onProgress && onProgress(done, chunkCount)

  // 2. 只补传缺失分片，并发 3
  const tasks = []
  for (let i = 0; i < chunkCount; i++) {
    if (uploaded.has(i)) continue
    tasks.push(async () => {
      const blob = file.slice(i * chunkSize, Math.min((i + 1) * chunkSize, file.size))
      const chunkMd5 = SparkMD5.ArrayBuffer.hash(await blob.arrayBuffer())
      await uploadChunk(uploadId, i, chunkMd5, blob)
      done++
      onProgress && onProgress(done, chunkCount)
    })
  }
  await runWithConcurrency(tasks, 3)

  return { uploadId, alreadyExists: false, videoId: null, chunkCount }
}
