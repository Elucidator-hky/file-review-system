<template>
  <div class="resubmit-page" v-loading="loading">
    <el-page-header content="再次提交" @back="goBack" class="mb-16" />

    <el-card v-if="oldVersion" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>上一版本信息</h3>
          <el-tag type="danger">已打回</el-tag>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ taskDetail?.taskName }}</el-descriptions-item>
        <el-descriptions-item label="版本号">v{{ oldVersion.versionNumber }}</el-descriptions-item>
        <el-descriptions-item label="审查意见" :span="2">
          {{ oldVersion.reviewComment || '无' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table
        class="mt-12"
        :data="oldFiles"
        border
        empty-text="暂无历史文件"
      >
        <el-table-column prop="fileName" label="文件名称" min-width="220" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="mt-16" shadow="never">
      <template #header>
        <h3>重新提交配置</h3>
      </template>
      <el-form label-width="100px">
        <el-form-item label="沿用旧文件">
          <el-switch v-model="reuseOldFiles" />
          <span class="hint">开启后自动复制上一版本文件，可在创建后继续上传新文件。</span>
        </el-form-item>
        <el-form-item label="提交说明">
          <el-input
            v-model="submitDesc"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="说明本次修改内容，最多2000字"
          />
        </el-form-item>
        <el-form-item v-if="!newVersionId">
          <el-button type="primary" :loading="initializing" @click="initResubmit">创建新版本</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="newVersionId" class="mt-16" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>上传新版本文件（v{{ newVersionNumber }}）</h3>
          <el-tag v-if="copying" type="warning">正在复制历史文件...</el-tag>
          <el-tag v-else type="success">文件已就绪</el-tag>
        </div>
      </template>
      <FileUpload :version-id="newVersionId" @uploaded="refreshStatus" @deleted="refreshStatus" />
      <div class="actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :disabled="copying" :loading="submitting" @click="submitResubmit">
          提交审核
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import {
  getTaskDetail,
  fetchVersionStatus,
  startResubmit,
  submitResubmit as submitResubmitApi
} from '@/api/task'
import { fetchFileList } from '@/api/file'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => Number(route.params.taskId))
const oldVersionId = computed(() => Number(route.query.versionId))

const loading = ref(false)
const initializing = ref(false)
const submitting = ref(false)
const taskDetail = ref(null)
const oldVersion = ref(null)
const oldFiles = ref([])
const reuseOldFiles = ref(true)
const submitDesc = ref('')
const newVersionId = ref(null)
const newVersionNumber = ref(null)
const copying = ref(false)
let pollingTimer = null

const loadDetail = async () => {
  loading.value = true
  try {
    const data = await getTaskDetail(taskId.value)
    taskDetail.value = data
    oldVersion.value = data?.versions?.find(v => v.versionId === oldVersionId.value) || null
    oldFiles.value = await fetchFileList(oldVersionId.value)
  } catch (error) {
    ElMessage.error(error?.message || '获取任务信息失败')
  } finally {
    loading.value = false
  }
}

const initResubmit = async () => {
  if (!oldVersionId.value) {
    ElMessage.error('缺少历史版本信息')
    return
  }
  initializing.value = true
  try {
    const response = await startResubmit(taskId.value, {
      oldVersionId: oldVersionId.value,
      reuseOldFiles: reuseOldFiles.value
    })
    newVersionId.value = response.versionId
    newVersionNumber.value = response.versionNumber
    if (reuseOldFiles.value) {
      startPolling()
    } else {
      copying.value = false
    }
  } catch (error) {
    ElMessage.error(error?.message || '创建新版本失败')
  } finally {
    initializing.value = false
  }
}

const startPolling = () => {
  copying.value = true
  stopPolling()
  pollingTimer = setInterval(async () => {
    await refreshStatus()
  }, 2000)
}

const refreshStatus = async () => {
  if (!newVersionId.value) return
  try {
    const status = await fetchVersionStatus(newVersionId.value)
    if (status?.filesReady === 1) {
      copying.value = false
      stopPolling()
    }
  } catch (error) {
    console.warn('查询版本状态失败', error)
  }
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const submitResubmit = async () => {
  if (!newVersionId.value) {
    ElMessage.warning('请先创建新版本')
    return
  }
  if (copying.value) {
    ElMessage.warning('文件仍在复制中，请稍候')
    return
  }
  submitting.value = true
  try {
    await submitResubmitApi(taskId.value, {
      versionId: newVersionId.value,
      submitDesc: submitDesc.value
    })
    ElMessage.success('已提交审核')
    goBack()
  } catch (error) {
    ElMessage.error(error?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const formatSize = (size) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const goBack = () => {
  stopPolling()
  router.push({ name: 'UserMyTasks' })
}

onMounted(() => {
  loadDetail()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.resubmit-page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb-16 {
  margin-bottom: 16px;
}

.mt-12 {
  margin-top: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.hint {
  margin-left: 12px;
  color: #909399;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
