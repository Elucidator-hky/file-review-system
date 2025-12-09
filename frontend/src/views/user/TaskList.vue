<template>
  <div class="task-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" label-width="80px" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="审查中" value="REVIEWING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已打回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="输入任务名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="goCreate">新建任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        empty-text="暂无任务，立即创建吧"
      >
        <el-table-column prop="taskName" label="任务名称" min-width="220">
          <template #default="{ row }">
            <span class="link" @click="openDetail(row)">{{ row.taskName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="当前版本" width="120">
          <template #default="{ row }">
            v{{ row.currentVersion || 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="审查员" width="160">
          <template #default="{ row }">
            {{ row.reviewerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="currentStatus" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="row.statusTagType">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileCount" label="文件数" width="90" />
        <el-table-column prop="lastUpdateTime" label="最近更新" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastUpdateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
            <el-button
              type="warning"
              link
              :disabled="!row.canResubmit"
              @click="handleResubmit(row)"
            >
              再次提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :page-size="pagination.pageSize"
          :current-page="pagination.pageNo"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <TaskDetailDrawer
      v-if="selectedTaskId"
      v-model="detailVisible"
      :task-id="selectedTaskId"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TaskDetailDrawer from '@/views/shared/TaskDetailDrawer.vue'
import { fetchMyTasks, fetchMyTaskStatistics } from '@/api/task'

const router = useRouter()

const filters = reactive({
  status: '',
  keyword: '',
  dateRange: []
})

const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const detailVisible = ref(false)
const selectedTaskId = ref(null)

const buildQueryParams = () => {
  const params = {
    pageNo: pagination.pageNo,
    pageSize: pagination.pageSize
  }
  if (filters.status) params.status = filters.status
  if (filters.keyword) params.keyword = filters.keyword.trim()
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

const loadTasks = async () => {
  loading.value = true
  try {
    const page = await fetchMyTasks(buildQueryParams())
    tableData.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.pageNo = Number(page.current || pagination.pageNo)
    pagination.pageSize = Number(page.size || pagination.pageSize)
  } catch (error) {
    ElMessage.error(error?.message || '获取任务列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadTasks()
}

const handleReset = () => {
  filters.status = ''
  filters.keyword = ''
  filters.dateRange = []
  pagination.pageNo = 1
  loadTasks()
}

const goCreate = () => {
  router.push({ name: 'UserCreateTask' })
}

const handlePageChange = (page) => {
  pagination.pageNo = page
  loadTasks()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNo = 1
  loadTasks()
}

const openDetail = (row) => {
  selectedTaskId.value = row.taskId
  detailVisible.value = true
}

const handleResubmit = (row) => {
  if (!row.canResubmit) return
  router.push({
    name: 'UserResubmitTask',
    params: { taskId: row.taskId },
    query: { versionId: row.currentVersionId }
  })
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.task-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  padding-bottom: 0;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.table-card {
  flex: 1;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.link {
  color: #409eff;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}
</style>
