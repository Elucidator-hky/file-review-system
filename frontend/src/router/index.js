import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resolveDefaultRoute, extractRequiredRoles, normalizeRoles } from './routeHelper'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  // 平台超管路由
  {
    path: '/platform',
    name: 'Platform',
    component: () => import('../layouts/PlatformLayout.vue'),
    meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'] },
    children: [
      {
        path: 'tenants',
        name: 'PlatformTenants',
        component: () => import('../views/platform/TenantManagement.vue'),
        meta: { title: '租户管理' }
      },
      {
        path: 'monitor/queues',
        name: 'PlatformQueueMonitor',
        component: () => import('../views/monitor/QueuePanel.vue'),
        meta: { title: '队列监控' }
      },
      {
        path: 'monitor/cache',
        name: 'PlatformCacheMonitor',
        component: () => import('../views/monitor/CachePanel.vue'),
        meta: { title: '缓存监控' }
      }
    ]
  },
  // 企业管理员路由
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['TENANT_ADMIN'] },
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManagement.vue'),
        meta: { title: '用户管理' }
      }
    ]
  },
  // 审查员路由
  {
    path: '/reviewer',
    name: 'Reviewer',
    component: () => import('../layouts/ReviewerLayout.vue'),
    meta: { requiresAuth: true, roles: ['REVIEWER'] },
    children: [
      {
        path: '',
        redirect: '/reviewer/dashboard'
      },
      {
        path: 'dashboard',
        name: 'ReviewerDashboard',
        component: () => import('../views/reviewer/Dashboard.vue'),
        meta: { title: '任务数据' }
      },
      {
        path: 'tasks',
        name: 'ReviewerTasks',
        component: () => import('../views/reviewer/TaskList.vue'),
        meta: { title: '待审查任务' }
      },
      {
        path: 'task/:taskId',
        name: 'ReviewerTaskDetail',
        component: () => import('../views/reviewer/ReviewTask.vue'),
        meta: { title: '审查任务' }
      }
    ]
  },
  // 普通用户路由
  {
    path: '/user',
    name: 'User',
    component: () => import('../layouts/UserLayout.vue'),
    meta: { requiresAuth: true, roles: ['USER'] },
    children: [
      {
        path: '',
        redirect: '/user/dashboard'
      },
      {
        path: 'dashboard',
        name: 'UserDashboard',
        component: () => import('../views/user/Dashboard.vue'),
        meta: { title: '任务数据' }
      },
      {
        path: 'my-tasks',
        name: 'UserMyTasks',
        component: () => import('../views/user/TaskList.vue'),
        meta: { title: '我的任务' }
      },
      {
        path: 'create-task',
        name: 'UserCreateTask',
        component: () => import('../views/user/CreateTask.vue'),
        meta: { title: '创建任务' }
      },
      {
        path: 'task/:taskId/resubmit',
        name: 'UserResubmitTask',
        component: () => import('../views/user/ResubmitTask.vue'),
        meta: { title: '再次提交' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const getStoredUserInfo = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) {
    return null
  }
  try {
    return JSON.parse(userInfoStr)
  } catch (error) {
    console.warn('用户信息解析失败，将清除缓存', error)
    localStorage.removeItem('userInfo')
    return null
  }
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token') || ''
  const userInfo = getStoredUserInfo()
  let currentRole = localStorage.getItem('currentRole') || ''
  const userRoles = normalizeRoles(userInfo?.roles)

  if (currentRole && !userRoles.includes(currentRole)) {
    currentRole = ''
    localStorage.removeItem('currentRole')
  }

  if (to.meta.requiresAuth !== false) {
    if (!token || !userInfo) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }

    const requiredRoles = extractRequiredRoles(to)

    if (requiredRoles.length && !userRoles.length) {
      ElMessage.error('账号暂无可用角色，请联系管理员')
      next('/login')
      return
    }

    if (requiredRoles.length && !requiredRoles.some(role => userRoles.includes(role))) {
      ElMessage.error('没有权限访问该页面')
      const fallback = resolveDefaultRoute(userRoles, currentRole)
      next(fallback === to.path ? '/login' : fallback)
      return
    }

    next()
  } else {
    if (to.path === '/login' && token && userInfo) {
      const targetRoute = resolveDefaultRoute(userRoles, currentRole)
      next(targetRoute)
      return
    }
    next()
  }
})

export default router
