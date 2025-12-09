import { normalizeRoles, pickDefaultRole } from '@/utils/role'

const ROLE_ROUTE_MAP = {
  PLATFORM_ADMIN: '/platform/tenants',
  TENANT_ADMIN: '/admin/users',
  REVIEWER: '/reviewer/dashboard',
  USER: '/user/dashboard'
}

/**
 * 根据角色返回默认路由
 * @param {string|string[]} roles 角色集合
 * @param {string} preferredRole 希望使用的角色
 * @returns {string} 对应路由
 */
export function resolveDefaultRoute(roles, preferredRole) {
  const normalizedRoles = normalizeRoles(roles)
  const roleToUse =
    preferredRole && normalizedRoles.includes(preferredRole)
      ? preferredRole
      : pickDefaultRole(normalizedRoles)
  return ROLE_ROUTE_MAP[roleToUse] || '/login'
}

/**
 * 根据 `to` 路由对象提取受限角色
 * @param {import('vue-router').RouteLocationNormalized} route
 * @returns {string[]} 需要的角色
 */
export function extractRequiredRoles(route) {
  if (route.meta && Array.isArray(route.meta.roles)) {
    return route.meta.roles
  }
  const matchedRecord = route.matched.find(record => Array.isArray(record.meta?.roles))
  return matchedRecord ? matchedRecord.meta.roles : []
}

export { normalizeRoles }
