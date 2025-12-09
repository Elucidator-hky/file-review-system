<template>
  <div class="tenant-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" label-width="80px">
        <el-form-item label="企业名称">
          <el-input
            v-model="filters.keyword"
            placeholder="请输入企业名称关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="全部" :value="undefined" />
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
        <el-button type="primary" @click="openCreateDialog">创建租户</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tenantList"
        border
        style="width: 100%"
      >
        <el-table-column prop="tenantName" label="企业名称" min-width="160" />
        <el-table-column label="联系人" min-width="160">
          <template #default="{ row }">
            <div class="link-text">{{ row.contactName }}</div>
            <div class="sub-text">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="140">
          <template #default="{ row }">
            <span>{{ row.userCount }}/{{ row.userQuota }}</span>
          </template>
        </el-table-column>
        <el-table-column label="存储空间" min-width="220">
          <template #default="{ row }">
            <div class="storage-text">
              {{ formatStorage(row.storageUsed) }} / {{ formatStorage(row.storageQuota) }}
            </div>
            <el-progress
              :percentage="calcStoragePercent(row)"
              :status="row.storageQuota === 0 ? undefined : (calcStoragePercent(row) > 90 ? 'exception' : undefined)"
              :stroke-width="14"
              striped
              striped-flow
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAdminManage(row)">管理员</el-button>
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              link
              type="warning"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              @click="confirmDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <div class="total-text">共 {{ pagination.total }} 条数据</div>
        <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pagination.pageSize"
          :current-page="pagination.pageNo"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '创建租户' : '编辑租户'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="tenantFormRef"
        :model="tenantForm"
        :rules="tenantRules"
        label-width="120px"
      >
        <el-form-item label="企业名称" prop="tenantName" v-if="dialogMode === 'create'">
          <el-input v-model="tenantForm.tenantName" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="企业名称" v-else>
          <el-input v-model="tenantForm.tenantName" disabled />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="tenantForm.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="tenantForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="存储配额(GB)" prop="storageQuotaGb">
          <el-input-number
            v-model="tenantForm.storageQuotaGb"
            :min="10"
            :step="10"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="用户配额" prop="userQuota">
          <el-input-number
            v-model="tenantForm.userQuota"
            :min="1"
            :step="5"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTenant">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="adminDialogVisible"
      :title="adminDialogMode === 'create' ? '新增管理员' : '编辑管理员'"
      width="480px"
      destroy-on-close
    >
      <el-form
        ref="adminFormRef"
        :model="adminForm"
        :rules="adminRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username" v-if="adminDialogMode === 'create'">
          <el-input v-model="adminForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="用户名" v-else>
          <el-input v-model="adminForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="adminForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="adminForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="adminDialogMode === 'create'">
          <el-input v-model="adminForm.password" placeholder="请输入初始密码" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdmin">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      title="租户详情"
      size="640px"
      @close="handleDetailClose"
    >
      <div class="detail-content" v-loading="detailLoading" element-loading-text="加载中...">
        <template v-if="detailData">
          <div class="detail-section">
            <div class="section-header">企业信息</div>
            <div class="detail-item">
              <span class="label">企业名称：</span>
              <span class="value">{{ detailData.tenantName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">联系人：</span>
              <span class="value">{{ detailData.contactName }} / {{ detailData.contactPhone }}</span>
            </div>
            <div class="detail-item">
              <span class="label">存储用量：</span>
              <span class="value">{{ formatStorage(detailData.storageUsed) }} / {{ formatStorage(detailData.storageQuota) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户数量：</span>
              <span class="value">{{ detailData.userCount }} / {{ detailData.userQuota }}</span>
            </div>
            <div class="detail-item">
              <span class="label">状态：</span>
              <el-tag :type="detailData.status === 1 ? 'success' : 'info'">
                {{ detailData.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDate(detailData.createTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">更新时间：</span>
              <span class="value">{{ formatDate(detailData.updateTime) }}</span>
            </div>
          </div>

        </template>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>

    <el-dialog
      v-model="adminManageVisible"
      :title="adminManageTitle"
      width="1100px"
      top="5vh"
      destroy-on-close
      class="admin-manage-dialog"
      @close="handleAdminManageClose"
    >
      <div class="admin-manage-content" v-loading="adminManageLoading" element-loading-text="加载中...">
        <template v-if="adminTenantInfo">
          <div class="detail-section">
            <div class="section-header">
              <span>企业信息</span>
              <el-tag :type="adminTenantInfo.status === 1 ? 'success' : 'info'">
                {{ adminTenantInfo.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">企业名称：</span>
                <span class="value">{{ adminTenantInfo.tenantName }}</span>
              </div>
              <div class="info-item">
                <span class="label">用户配额：</span>
                <span class="value">{{ adminTenantInfo.userCount }} / {{ adminTenantInfo.userQuota }}</span>
              </div>
              <div class="info-item">
                <span class="label">存储配额：</span>
                <span class="value">{{ formatStorage(adminTenantInfo.storageUsed) }} / {{ formatStorage(adminTenantInfo.storageQuota) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-header">
              <span>企业管理员</span>
              <div class="section-actions">
                <el-button type="primary" @click="openAdminDialog('create')" :disabled="isAdminTenantDisabled">
                  新增管理员
                </el-button>
              </div>
            </div>
            <p class="section-tip">管理员账号用于登录企业后台，创建成功后可单独启停或重置密码。</p>
            <p class="section-tip error" v-if="isAdminTenantDisabled">租户已停用，暂无法操作管理员。</p>
            <el-table
              v-loading="adminLoading"
              :data="adminList"
              border
              :empty-text="'暂无管理员'"
            >
              <el-table-column prop="username" label="用户名" width="140" />
              <el-table-column prop="realName" label="真实姓名" width="140" />
              <el-table-column prop="phone" label="手机号" width="160" />
              <el-table-column label="最近登录" min-width="180">
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
              <el-table-column label="操作" width="240">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openAdminDialog('edit', row)" :disabled="isAdminTenantDisabled">
                    编辑
                  </el-button>
                  <el-button link type="primary" size="small" @click="handleResetPassword(row)" :disabled="isAdminTenantDisabled">
                    重置密码
                  </el-button>
                  <el-button link type="warning" size="small" @click="toggleAdminStatus(row)" :disabled="isAdminTenantDisabled">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <el-empty v-else description="请选择租户" />
      </div>
      <template #footer>
        <el-button @click="adminManageVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchTenantList,
  createTenant,
  updateTenant,
  updateTenantStatus,
  deleteTenant,
  getTenantDetail,
  fetchTenantAdmins,
  createTenantAdmin,
  updateTenantAdmin,
  resetTenantAdminPassword,
  updateTenantAdminStatus
} from '@/api/tenant'

const GB_UNIT = 1024 * 1024 * 1024

const loading = ref(false)
const tenantList = ref([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  status: undefined
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const tenantFormRef = ref(null)
const tenantForm = reactive({
  id: null,
  tenantName: '',
  contactName: '',
  contactPhone: '',
  storageQuotaGb: 100,
  userQuota: 50
})

const tenantRules = {
  tenantName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度需在2~100个字符', trigger: 'blur' }
  ],
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度需在2~50个字符', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需在3~20个字符', trigger: 'blur' }
  ],
  storageQuotaGb: [
    { required: true, message: '请设置存储配额', trigger: 'change' },
    { type: 'number', min: 10, message: '至少10GB', trigger: 'change' }
  ],
  userQuota: [
    { required: true, message: '请设置用户配额', trigger: 'change' },
    { type: 'number', min: 1, message: '至少1个账号', trigger: 'change' }
  ]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const currentTenantId = ref(null)

const adminTenantId = ref(null)
const adminManageVisible = ref(false)
const adminManageLoading = ref(false)
const adminTenantInfo = ref(null)
const adminList = ref([])
const adminLoading = ref(false)
const adminManageTitle = computed(() => {
  if (!adminTenantInfo.value) {
    return '管理员管理'
  }
  return `${adminTenantInfo.value.tenantName} - 管理员管理`
})
const isAdminTenantDisabled = computed(() => adminTenantInfo.value?.status === 0)

const adminDialogVisible = ref(false)
const adminDialogMode = ref('create')
const adminFormRef = ref(null)
const adminForm = reactive({
  id: null,
  username: '',
  realName: '',
  phone: '',
  password: ''
})

const adminRules = {
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
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,32}$/,
      message: '密码需包含字母和数字，长度6-32位',
      trigger: 'blur'
    }
  ]
}

const loadTenants = async () => {
  loading.value = true
  try {
    const params = {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status
    }
    const data = await fetchTenantList(params)
    tenantList.value = data.records || []
    pagination.total = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadTenants()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = undefined
  handleSearch()
}

const handlePageChange = (page) => {
  pagination.pageNo = page
  loadTenants()
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  Object.assign(tenantForm, {
    id: null,
    tenantName: '',
    contactName: '',
    contactPhone: '',
    storageQuotaGb: 100,
    userQuota: 50
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  Object.assign(tenantForm, {
    id: row.id,
    tenantName: row.tenantName,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    storageQuotaGb: bytesToGb(row.storageQuota),
    userQuota: row.userQuota
  })
  dialogVisible.value = true
}

const submitTenant = async () => {
  if (!tenantFormRef.value) return
  await tenantFormRef.value.validate()
  const payload = {
    tenantName: tenantForm.tenantName,
    contactName: tenantForm.contactName,
    contactPhone: tenantForm.contactPhone,
    storageQuota: gbToBytes(tenantForm.storageQuotaGb),
    userQuota: tenantForm.userQuota
  }
  if (dialogMode.value === 'create') {
    await createTenant(payload)
    ElMessage.success('租户创建成功')
  } else {
    await updateTenant(tenantForm.id, payload)
    ElMessage.success('租户信息已更新')
  }
  dialogVisible.value = false
  loadTenants()
  if (currentTenantId.value && tenantForm.id === currentTenantId.value) {
    await refreshTenantDetail()
  }
}

const toggleStatus = async (row) => {
  const targetStatus = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定要${targetStatus === 1 ? '启用' : '停用'}租户「${row.tenantName}」吗？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )
  await updateTenantStatus(row.id, { status: targetStatus })
  ElMessage.success('状态更新成功')
  loadTenants()
  if (currentTenantId.value === row.id) {
    await refreshTenantDetail()
  }
}

const confirmDelete = async (row) => {
  await ElMessageBox.confirm(
    `删除后将无法恢复，并会清空该企业所有数据。是否确认删除「${row.tenantName}」？`,
    '警告',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )
  await deleteTenant(row.id)
  ElMessage.success('删除成功')
  loadTenants()
  if (currentTenantId.value === row.id) {
    detailVisible.value = false
  }
}

const refreshTenantDetail = async () => {
  if (!currentTenantId.value) return
  const data = await getTenantDetail(currentTenantId.value)
  detailData.value = data
}

const loadTenantAdmins = async (tenantId) => {
  const targetId = tenantId ?? adminTenantId.value
  if (targetId === null || targetId === undefined) return
  adminLoading.value = true
  try {
    const list = await fetchTenantAdmins(targetId)
    adminList.value = list || []
  } finally {
    adminLoading.value = false
  }
}

const openDetail = async (row) => {
  currentTenantId.value = row.id
  detailVisible.value = true
  detailLoading.value = true
  try {
    await refreshTenantDetail()
  } finally {
    detailLoading.value = false
  }
}

const handleDetailClose = () => {
  detailData.value = null
  currentTenantId.value = null
}

const refreshAdminTenantInfo = async () => {
  if (adminTenantId.value === null || adminTenantId.value === undefined) return
  const data = await getTenantDetail(adminTenantId.value)
  adminTenantInfo.value = data
}

const openAdminManage = async (row) => {
  adminTenantId.value = row.id
  adminManageVisible.value = true
  adminManageLoading.value = true
  try {
    await refreshAdminTenantInfo()
    await loadTenantAdmins(row.id)
  } finally {
    adminManageLoading.value = false
  }
}

const handleAdminManageClose = () => {
  adminManageVisible.value = false
  adminTenantInfo.value = null
  adminList.value = []
  adminTenantId.value = null
}

const openAdminDialog = (mode, admin) => {
  if (!adminManageVisible.value || adminTenantId.value === null || adminTenantId.value === undefined) {
    ElMessage.warning('请先打开管理员管理')
    return
  }
  if (isAdminTenantDisabled.value) {
    ElMessage.warning('租户已停用，无法操作管理员')
    return
  }
  adminDialogMode.value = mode
  if (mode === 'create') {
    Object.assign(adminForm, {
      id: null,
      username: '',
      realName: '',
      phone: '',
      password: ''
    })
  } else if (admin) {
    Object.assign(adminForm, {
      id: admin.id,
      username: admin.username,
      realName: admin.realName,
      phone: admin.phone,
      password: ''
    })
  }
  adminDialogVisible.value = true
}

const submitAdmin = async () => {
  if (!adminFormRef.value || adminTenantId.value === null || adminTenantId.value === undefined) return
  await adminFormRef.value.validate()
  if (adminDialogMode.value === 'create') {
    await createTenantAdmin(adminTenantId.value, {
      username: adminForm.username,
      realName: adminForm.realName,
      phone: adminForm.phone,
      password: adminForm.password
    })
    ElMessage.success('管理员创建成功')
  } else {
    await updateTenantAdmin(adminTenantId.value, adminForm.id, {
      realName: adminForm.realName,
      phone: adminForm.phone
    })
    ElMessage.success('管理员信息已更新')
  }
  adminDialogVisible.value = false
  await loadTenantAdmins()
  await refreshAdminTenantInfo()
}

const handleResetPassword = async (admin) => {
  if (adminTenantId.value === null || adminTenantId.value === undefined) return
  if (isAdminTenantDisabled.value) {
    ElMessage.warning('租户已停用，无法操作管理员')
    return
  }
  const { value } = await ElMessageBox.prompt(
    `请输入管理员「${admin.username}」的新密码（需包含字母和数字）`,
    '重置密码',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPlaceholder: '请输入6-32位新密码',
      inputPattern: new RegExp('^(?=.*[A-Za-z])(?=.*\\d).{6,32}$'),
      inputErrorMessage: '密码需包含字母和数字，长度6-32位'
    }
  )
  await resetTenantAdminPassword(adminTenantId.value, admin.id, { newPassword: value })
  ElMessage.success('密码重置成功')
}

const toggleAdminStatus = async (admin) => {
  if (adminTenantId.value === null || adminTenantId.value === undefined) return
  if (isAdminTenantDisabled.value) {
    ElMessage.warning('租户已停用，无法操作管理员')
    return
  }
  const targetStatus = admin.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定要${targetStatus === 1 ? '启用' : '停用'}管理员「${admin.username}」吗？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )
  await updateTenantAdminStatus(adminTenantId.value, admin.id, { status: targetStatus })
  ElMessage.success('管理员状态已更新')
  await loadTenantAdmins()
}

const gbToBytes = (value) => Math.round(Number(value || 0) * GB_UNIT)
const bytesToGb = (value) => Number(((value || 0) / GB_UNIT).toFixed(2))

const formatStorage = (value) => {
  if (!value) return '0 GB'
  return `${bytesToGb(value)} GB`
}

const calcStoragePercent = (row) => {
  if (!row.storageQuota) return 0
  const percent = (row.storageUsed / row.storageQuota) * 100
  return Math.min(100, Math.round(percent))
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

loadTenants()
</script>

<style scoped>
.tenant-page {
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
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.total-text {
  color: #666;
  font-size: 14px;
}

.link-text {
  font-weight: 500;
}

.sub-text {
  font-size: 12px;
  color: #999;
}

.storage-text {
  font-size: 12px;
  margin-bottom: 4px;
}

.detail-content {
  padding: 8px 4px 16px;
}

.admin-manage-content {
  padding: 8px 4px 16px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-header {
  font-weight: 600;
  font-size: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.section-tip {
  font-size: 12px;
  color: #909399;
  margin: 0 0 6px;
}

.section-tip.error {
  color: #f56c6c;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.detail-item .label {
  width: 120px;
  font-weight: 500;
  color: #666;
}

.detail-item .value {
  flex: 1;
  color: #333;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin-bottom: 8px;
}

.info-item {
  display: flex;
  gap: 6px;
  font-size: 14px;
}

.info-item .label {
  color: #666;
  white-space: nowrap;
}
</style>
