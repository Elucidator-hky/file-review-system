import request from '@/utils/request'

export const fetchQueueMetrics = () => {
  return request({
    url: '/monitor/queues',
    method: 'get'
  })
}

export const fetchCacheMetrics = () => {
  return request({
    url: '/monitor/cache',
    method: 'get'
  })
}
