/**
 * 文件 MD5 计算工具，对外提供 Promise + 进度回调 + 取消能力。
 */

const WORKER_PATH = '/workers/md5Worker.js'
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024
let sequence = 0

const createWorker = () => {
  try {
    return new Worker(WORKER_PATH)
  } catch (error) {
    throw new Error('无法创建 MD5 计算线程，请检查浏览器兼容性')
  }
}

/**
 * 计算文件 MD5。
 * @param {File} file 目标文件
 * @param {Object} options 配置
 * @param {number} options.chunkSize 分片大小，默认 5MB
 * @param {(progress: {percent:number,loaded:number,total:number}) => void} options.onProgress 进度回调
 * @returns {{promise: Promise<string>, cancel: () => void}}
 */
export function calculateFileMd5(file, options = {}) {
  if (!file) {
    throw new Error('未选择文件，无法计算 MD5')
  }

  const { chunkSize = DEFAULT_CHUNK_SIZE, onProgress } = options
  const worker = createWorker()
  const requestId = `md5-${Date.now()}-${sequence += 1}`

  const cleanup = () => {
    worker.terminate()
  }

  const promise = new Promise((resolve, reject) => {
    worker.onmessage = (event) => {
      const { type, hash, message, progress, loaded, total, requestId: rid } = event.data || {}
      if (rid && rid !== requestId) {
        return
      }

      switch (type) {
        case 'progress':
          onProgress?.({
            percent: progress,
            loaded,
            total
          })
          break
        case 'success':
          cleanup()
          resolve(hash)
          break
        case 'cancelled':
          cleanup()
          reject(new Error('MD5 计算已取消'))
          break
        case 'error':
        default:
          cleanup()
          reject(new Error(message || 'MD5 计算失败'))
          break
      }
    }

    worker.onerror = (error) => {
      cleanup()
      reject(error)
    }

    worker.postMessage({
      type: 'start',
      requestId,
      file,
      chunkSize
    })
  })

  const cancel = () => {
    worker.postMessage({ type: 'cancel', requestId })
  }

  return { promise, cancel }
}
