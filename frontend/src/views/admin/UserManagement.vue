<template>
  <div class="user-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" label-width="70px">
        <el-form-item label="用户名">
          <el-input
            v-model="filters.keyword"
            placeholder="请输入用户名/姓名/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.roles" multiple collapse-tags placeholder="全部角色" style="width: 220px">
            <el-option label="企业管理员" value="TENANT_ADMIN" />
            <el-option label="审查员" value="REVIEWER" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <el-button type="primary" @click="openCreateDialog">创建用户</el-button>
          <el-button :disabled="!selectedIds.length" @click="batchUpdateStatus(1)">批量启用</el-button>
          <el-button :disabled="!selectedIds.length" @click="batchUpdateStatus(0)">批量停用</el-button>
        </div>
        <div class="quota-info">
          <span>用户配额：{{ tenantInfo.userCount }} / {{ tenantInfo.userQuota }}</span>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="userList"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roles || []"
              :key="role"
              type="info"
              class="role-tag"
            >
              {{ formatRole(role) }}
            </el-tag>
            <el-tag v-if="isDualRole(row.roles)" type="success" class="role-tag">双角色</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="160">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              link
              type="warning"
              @click="updateStatus(row, row.status === 1 ? 0 : 1)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="pagination.pageNo"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '创建用户' : '编辑用户'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="用户名" prop="username" v-if="dialogMode === 'create'">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="用户名" v-else>
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-checkbox-group v-model="form.roles" @change="handleRoleChange">
            <el-checkbox label="TENANT_ADMIN">企业管理员</el-checkbox>
            <el-checkbox label="REVIEWER">审查员</el-checkbox>
            <el-checkbox label="USER">普通用户</el-checkbox>
          </el-checkbox-group>
          <div class="role-tip">
            企业管理员不可同时拥有其他角色；审查员可与普通用户同时勾选。
          </div>
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="dialogMode === 'create'">
          <el-input v-model="form.password" placeholder="请输入初始密码" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTenantDetail } from '@/api/tenant'
import {
  fetchUserList,
  createUser,
  updateUser,
  resetUserPassword,
  updateUserStatus,
  batchUpdateUserStatus
} from '@/api/user'

const tenantInfo = reactive({ userCount: 0, userQuota: 0 })
const currentTenantId = ref(null)

const filters = reactive({ keyword: '', roles: [], status: undefined })
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const userList = ref([])
const selectedIds = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  realName: '',
  phone: '',
  roles: [],
  password: ''
})

const isCreateMode = computed(() => dialogMode.value === 'create')

const passwordValidator = (rule, value, callback) => {
  if (isCreateMode.value) {
    if (!value) {
      callback(new Error('请输入初始密码'))
      return
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d).{6,32}$/.test(value)) {
      callback(new Error('密码需包含字母和数字，长度6-32位'))
      return
    }
  }
  callback()
}

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需在3~20个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度需在2~50个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需在3~20个字符', trigger: 'blur' }
  ],
  roles: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { validator: passwordValidator, trigger: 'blur' }
  ]
}

const initTenantContext = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) {
    ElMessage.warning('无法获取租户信息，请重新登录')
    return
  }
  try {
    const userInfo = JSON.parse(userInfoStr)
    currentTenantId.value = userInfo?.tenantId || null
    if (!currentTenantId.value) {
      ElMessage.warning('当前账号未绑定租户')
    }
  } catch (error) {
    currentTenantId.value = null
    ElMessage.error('用户信息解析失败，请重新登录')
  }
}
initTenantContext()

const loadTenantInfo = async () => {
  if (!currentTenantId.value) return
  const info = await getTenantDetail(currentTenantId.value)
  tenantInfo.userCount = info.userCount
  tenantInfo.userQuota = info.userQuota
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status,
      roles: filters.roles
    }
    const data = await fetchUserList(params)
    userList.value = data.records || []
    pagination.total = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.roles = []
  filters.status = undefined
  handleSearch()
}

const handlePageChange = (page) => {
  pagination.pageNo = page
  loadUsers()
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(row => row.id)
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  Object.assign(form, {
    id: null,
    username: '',
    realName: '',
    phone: '',
    roles: [],
    password: ''
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    roles: [...(row.roles || [])],
    password: ''
  })
  dialogVisible.value = true
}

const handleRoleChange = (roles) => {
  if (roles.includes('TENANT_ADMIN')) {
    form.roles = ['TENANT_ADMIN']
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  const payload = {
    username: form.username,
    realName: form.realName,
    phone: form.phone,
    roles: form.roles
  }
  if (dialogMode.value === 'create') {
    payload.password = form.password
    await createUser(payload)
    ElMessage.success('用户创建成功')
  } else {
    await updateUser(form.id, payload)
    ElMessage.success('用户信息已更新')
  }
  dialogVisible.value = false
  loadUsers()
  loadTenantInfo()
}

const handleResetPassword = async (row) => {
  const { value } = await ElMessageBox.prompt(
    `请输入用户「${row.username}」的新密码（需包含字母和数字）`,
    '重置密码',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPlaceholder: '请输入6-32位新密码',
      inputPattern: new RegExp('^(?=.*[A-Za-z])(?=.*\d).{6,32}$'),
      inputErrorMessage: '密码需包含字母和数字，长度6-32位'
    }
  )
  await resetUserPassword(row.id, { newPassword: value })
  ElMessage.success('密码重置成功')
}

const updateStatus = async (row, status) => {
  await ElMessageBox.confirm(
    `确定要${status === 1 ? '启用' : '停用'}用户「${row.username}」吗？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )
  await updateUserStatus(row.id, { status })
  ElMessage.success('状态更新成功')
  loadUsers()
}

const batchUpdateStatus = async (status) => {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(
    `将对选中的 ${selectedIds.value.length} 个用户执行${status === 1 ? '启用' : '停用'}，是否继续？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )
  await batchUpdateUserStatus({ userIds: selectedIds.value, status })
  ElMessage.success('批量操作成功')
  selectedIds.value = []
  loadUsers()
}

const formatRole = (role) => {
  switch (role) {
    case 'TENANT_ADMIN':
      return '企业管理员'
    case 'REVIEWER':
      return '审查员'
    case 'USER':
      return '普通用户'
    default:
      return role
  }
}

const isDualRole = (roles) => {
  if (!roles) return false
  return roles.includes('REVIEWER') && roles.includes('USER')
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

if (currentTenantId.value) {
  loadTenantInfo()
}
loadUsers()
</script>

<style scoped>
.user-page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  padding-bottom: 8px;
}

.table-card {
  flex: 1;
}

.table-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quota-info {
  color: #666;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.role-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.role-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
