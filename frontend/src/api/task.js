import request from '@/utils/request'

export function fetchReviewers() {
  return request({
    url: '/tasks/reviewers',
    method: 'get'
  })
}

export function createTask(data) {
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

export function startResubmit(taskId, data) {
  return request({
    url: `/tasks/${taskId}/resubmit/start`,
    method: 'post',
    data
  })
}

export function submitResubmit(taskId, data) {
  return request({
    url: `/tasks/${taskId}/resubmit/submit`,
    method: 'post',
    data
  })
}

export function fetchMyTasks(params) {
  return request({
    url: '/tasks/mine',
    method: 'get',
    params
  })
}

export function fetchReviewerTasks(params) {
  return request({
    url: '/tasks/reviewer',
    method: 'get',
    params
  })
}

export function fetchMyTaskStatistics() {
  return request({
    url: '/tasks/mine/statistics',
    method: 'get'
  })
}

export function fetchReviewerTaskStatistics() {
  return request({
    url: '/tasks/reviewer/statistics',
    method: 'get'
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: `/tasks/${taskId}`,
    method: 'get'
  })
}

export function getTaskVersions(taskId) {
  return request({
    url: `/tasks/${taskId}/versions`,
    method: 'get'
  })
}

export function fetchVersionStatus(versionId) {
  return request({
    url: `/tasks/version/${versionId}/status`,
    method: 'get'
  })
}
