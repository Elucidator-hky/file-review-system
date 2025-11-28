import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

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
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = null
  }

  // 登出
  const logout = () => {
    clearToken()
    clearUserInfo()
  }

  return {
    token,
    userInfo,
    setToken,
    clearToken,
    setUserInfo,
    clearUserInfo,
    logout
  }
})
