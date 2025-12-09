import request from '@/utils/request'

export function fetchTenantList(params) {
  return request({
    url: '/tenant/list',
    method: 'get',
    params
  })
}

export function createTenant(data) {
  return request({
    url: '/tenant',
    method: 'post',
    data
  })
}

export function updateTenant(id, data) {
  return request({
    url: `/tenant/${id}`,
    method: 'put',
    data
  })
}

export function updateTenantStatus(id, data) {
  return request({
    url: `/tenant/${id}/status`,
    method: 'put',
    data
  })
}

export function deleteTenant(id) {
  return request({
    url: `/tenant/${id}`,
    method: 'delete'
  })
}

export function getTenantDetail(id) {
  return request({
    url: `/tenant/${id}`,
    method: 'get'
  })
}

export function fetchTenantAdmins(tenantId) {
  return request({
    url: `/tenant/${tenantId}/admins`,
    method: 'get'
  })
}

export function createTenantAdmin(tenantId, data) {
  return request({
    url: `/tenant/${tenantId}/admins`,
    method: 'post',
    data
  })
}

export function updateTenantAdmin(tenantId, adminId, data) {
  return request({
    url: `/tenant/${tenantId}/admins/${adminId}`,
    method: 'put',
    data
  })
}

export function resetTenantAdminPassword(tenantId, adminId, data) {
  return request({
    url: `/tenant/${tenantId}/admins/${adminId}/reset-password`,
    method: 'post',
    data
  })
}

export function updateTenantAdminStatus(tenantId, adminId, data) {
  return request({
    url: `/tenant/${tenantId}/admins/${adminId}/status`,
    method: 'put',
    data
  })
}
