<template>
  <div class="reviewer-layout">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <h1 class="system-title">审查员</h1>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userStore.userInfo?.realName || '审查员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isDualRole" command="switchRole" divided>切换到普通用户</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <!-- 左侧菜单 -->
        <el-aside width="200px" class="layout-aside">
          <el-menu
            :default-active="$route.path"
            router
            class="layout-menu"
          >
            <el-menu-item index="/reviewer/dashboard">
              <el-icon><DataAnalysis /></el-icon>
              <span>任务数据</span>
            </el-menu-item>
            <el-menu-item index="/reviewer/tasks">
              <el-icon><Document /></el-icon>
              <span>待审查任务</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区 -->
        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- 个人信息对话框 -->
    <el-dialog
      v-model="showProfileDialog"
      title="个人信息"
      width="500px"
    >
      <div class="profile-info">
        <div class="info-item">
          <span class="label">用户名：</span>
          <span class="value">{{ userStore.userInfo?.username }}</span>
        </div>
        <div class="info-item">
          <span class="label">真实姓名：</span>
          <span class="value">{{ userStore.userInfo?.realName }}</span>
        </div>
        <div class="info-item">
          <span class="label">角色：</span>
          <span class="value">{{ userStore.userInfo?.roles }}</span>
        </div>
        <div class="info-item">
          <span class="label">租户ID：</span>
          <span class="value">{{ userStore.userInfo?.tenantId }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showProfileDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, ArrowDown, Document, DataAnalysis } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const showProfileDialog = ref(false)

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    showProfileDialog.value = true
  } else if (command === 'switchRole') {
    // 切换到普通用户角色
    if (userStore.switchRole('USER')) {
      ElMessage.success('已切换到普通用户')
      router.push('/user/my-tasks')
    }
  }
}
</script>

<style scoped>
.reviewer-layout {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.el-container {
  width: 100%;
  height: 100%;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
}

.header-left .system-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e8e8e8;
}

.layout-menu {
  border: none;
  height: 100%;
}

.layout-main {
  background: #f5f5f5;
  overflow: auto;
}

/* 个人信息对话框样式 */
.profile-info {
  padding: 20px 0;
}

.info-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  flex: 0 0 120px;
  color: #666;
  font-weight: 500;
}

.info-item .value {
  flex: 1;
  color: #333;
}
</style>
