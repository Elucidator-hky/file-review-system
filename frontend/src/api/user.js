import request from '@/utils/request'

export function fetchUserList(params) {
  return request({
    url: '/admin/users/list',
    method: 'get',
    params
  })
}

export function createUser(data) {
  return request({
    url: '/admin/users/create',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/admin/users/${id}`,
    method: 'put',
    data
  })
}

export function resetUserPassword(id, data) {
  return request({
    url: `/admin/users/${id}/reset-password`,
    method: 'post',
    data
  })
}

export function updateUserStatus(id, data) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data
  })
}

export function batchUpdateUserStatus(data) {
  return request({
    url: '/admin/users/status/batch',
    method: 'put',
    data
  })
}
