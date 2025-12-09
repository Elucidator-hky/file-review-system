import request from '@/utils/request'

export function fetchReviewDetail(versionId) {
  return request({
    url: `/review/${versionId}`,
    method: 'get'
  })
}

export function approveReview(versionId, data) {
  return request({
    url: `/review/${versionId}/approve`,
    method: 'post',
    data
  })
}

export function rejectReview(versionId, data) {
  return request({
    url: `/review/${versionId}/reject`,
    method: 'post',
    data
  })
}
