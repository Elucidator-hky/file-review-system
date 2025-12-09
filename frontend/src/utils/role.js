/**
 * 规范化角色列表，无论输入是字符串还是数组，都转换为大写、无空格的数组
 * @param {string|string[]} roles 输入的角色
 * @returns {string[]} 规范化后的角色数组
 */
export function normalizeRoles(roles) {
  if (!roles) {
    return []
  }

  if (Array.isArray(roles)) {
    return roles
      .map(role => (typeof role === 'string' ? role.trim().toUpperCase() : ''))
      .filter(Boolean)
  }

  if (typeof roles === 'string') {
    return roles
      .split(',')
      .map(role => role.trim().toUpperCase())
      .filter(Boolean)
  }

  return []
}

/**
 * 选出默认角色，优先普通用户，其次第一个可用角色
 * @param {string|string[]} roles 输入的角色
 * @returns {string} 默认角色
 */
export function pickDefaultRole(roles) {
  const normalized = normalizeRoles(roles)
  if (normalized.includes('USER')) {
    return 'USER'
  }
  return normalized[0] || ''
}
