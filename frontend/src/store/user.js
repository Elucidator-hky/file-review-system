import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { normalizeRoles, pickDefaultRole } from '@/utils/role'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')

  // 从 localStorage 初始化 userInfo
  const storedUserInfo = localStorage.getItem('userInfo')
  const userInfo = ref(storedUserInfo ? JSON.parse(storedUserInfo) : null)

  // 当前激活的角色
  const currentRole = ref(localStorage.getItem('currentRole') || '')

  // 设置 Token
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 清除 Token
  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('token')
  }

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))

    const resolvedRole = info ? pickDefaultRole(info.roles) : ''
    currentRole.value = resolvedRole

    if (resolvedRole) {
      localStorage.setItem('currentRole', resolvedRole)
    } else {
      localStorage.removeItem('currentRole')
    }

    return resolvedRole
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = null
    localStorage.removeItem('userInfo')
    currentRole.value = ''
    localStorage.removeItem('currentRole')
  }

  // 判断是否为双角色用户（REVIEWER和USER）
  const isDualRole = computed(() => {
    if (!userInfo.value || !userInfo.value.roles) return false
    const roles = userInfo.value.roles.split(',')
    return roles.includes('REVIEWER') && roles.includes('USER')
  })

  // 获取用户所有角色列表
  const roleList = computed(() => {
    if (!userInfo.value || !userInfo.value.roles) return []
    return normalizeRoles(userInfo.value.roles)
  })

  // 切换角色
  const switchRole = (role) => {
    if (!roleList.value.includes(role)) {
      console.error('用户没有该角色权限')
      return false
    }
    currentRole.value = role
    localStorage.setItem('currentRole', role)
    return true
  }

  // 获取当前角色的显示名称
  const currentRoleLabel = computed(() => {
    const roleMap = {
      'PLATFORM_ADMIN': '平台管理员',
      'TENANT_ADMIN': '租户管理员',
      'REVIEWER': '审查员',
      'USER': '普通用户'
    }
    return roleMap[currentRole.value] || '未知角色'
  })

  // 登出
  const logout = () => {
    clearToken()
    clearUserInfo()
  }

  return {
    token,
    userInfo,
    currentRole,
    isDualRole,
    roleList,
    currentRoleLabel,
    setToken,
    clearToken,
    setUserInfo,
    clearUserInfo,
    switchRole,
    logout
  }
})
