<template>
  <div class="login-container">
    <!-- 左侧展示区域 -->
    <div class="login-left">
      <div class="left-content">
        <h1 class="system-title">文件审查系统</h1>
        <p class="system-subtitle">File Review System</p>
        <div class="system-description">
          <p>高效、安全、专业的文件审查管理平台</p>
          <ul class="feature-list">
            <li>支持多租户管理</li>
            <li>完善的权限控制</li>
            <li>文件在线预览</li>
            <li>审查流程管理</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="login-right">
      <div class="login-box">
        <h2 class="login-title">用户登录</h2>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <p>测试账号：admin / admin123</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { login } from '@/api/auth'
import { resolveDefaultRoute } from '@/router/routeHelper'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref(null)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ]
}

// 登录加载状态
const loading = ref(false)

/**
 * 处理登录
 */
const handleLogin = async () => {
  // 验证表单
  if (!loginFormRef.value) return

  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true

  try {
    // 调用登录接口
    const response = await login({
      username: loginForm.username,
      password: loginForm.password
    })

    // 保存 token 和用户信息
    userStore.setToken(response.token)
    const assignedRole = userStore.setUserInfo({
      userId: response.userId,
      username: response.username,
      realName: response.realName,
      tenantId: response.tenantId,
      roles: response.roles
    })

    ElMessage.success('登录成功')

    // 根据角色跳转至对应页面
    router.push(resolveDefaultRoute(response.roles, assignedRole))
  } catch (error) {
    console.error('登录失败:', error)
    // 错误已经在 request.js 的拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录容器 - 全屏左右分屏 */
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  overflow: hidden;
}

/* 左侧展示区域 - 占60%宽度 */
.login-left {
  flex: 0 0 60%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: white;
}

.left-content {
  max-width: 600px;
}

.system-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 20px 0;
  letter-spacing: 2px;
}

.system-subtitle {
  font-size: 24px;
  margin: 0 0 40px 0;
  opacity: 0.9;
  font-weight: 300;
  letter-spacing: 1px;
}

.system-description {
  margin-top: 60px;
}

.system-description > p {
  font-size: 20px;
  margin-bottom: 30px;
  opacity: 0.95;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  font-size: 18px;
  padding: 12px 0;
  padding-left: 30px;
  position: relative;
  opacity: 0.9;
}

.feature-list li::before {
  content: '✓';
  position: absolute;
  left: 0;
  font-weight: bold;
  font-size: 20px;
}

/* 右侧登录区域 - 占40%宽度 */
.login-right {
  flex: 0 0 40%;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.login-box {
  width: 100%;
  max-width: 420px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 40px 0;
  text-align: center;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 12px 15px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 1px;
  margin-top: 10px;
}

.login-footer {
  margin-top: 30px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.login-footer p {
  margin: 0;
  font-size: 13px;
  color: #999;
}
</style>
