/* eslint-disable no-undef */
// 该 worker 专门用于大文件 MD5 计算，避免阻塞主线程。
// 依赖 spark-md5，在 public/libs 下内置一份压缩脚本。
self.importScripts('/libs/spark-md5.min.js')

const CHUNK_SIZE = 5 * 1024 * 1024 // 默认分片大小 5MB
let currentRequestId = null
let cancelled = false

/**
 * 对单个文件执行分片 MD5 计算。
 * @param {File} file 目标文件
 * @param {number} chunkSize 分片大小
 */
const processFile = (file, chunkSize = CHUNK_SIZE) => {
  const totalChunks = Math.ceil(file.size / chunkSize)
  const spark = new self.SparkMD5.ArrayBuffer()
  const reader = new FileReaderSync()

  for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
    if (cancelled) {
      self.postMessage({ type: 'cancelled', requestId: currentRequestId })
      return
    }

    const start = chunkIndex * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    const chunk = file.slice(start, end)
    const buffer = reader.readAsArrayBuffer(chunk)
    spark.append(buffer)

    self.postMessage({
      type: 'progress',
      requestId: currentRequestId,
      progress: Math.round(((chunkIndex + 1) / totalChunks) * 100),
      loaded: end,
      total: file.size
    })
  }

  const hash = spark.end()
  self.postMessage({
    type: 'success',
    requestId: currentRequestId,
    hash
  })
}

self.onmessage = (event) => {
  const { data } = event

  if (!data || !data.type) {
    return
  }

  if (data.type === 'start') {
    currentRequestId = data.requestId
    cancelled = false
    try {
      processFile(data.file, data.chunkSize)
    } catch (error) {
      self.postMessage({
        type: 'error',
        requestId: currentRequestId,
        message: error?.message || '计算 MD5 失败'
      })
    }
  } else if (data.type === 'cancel' && currentRequestId === data.requestId) {
    cancelled = true
  }
}
