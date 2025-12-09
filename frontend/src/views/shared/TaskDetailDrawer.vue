<template>
  <el-dialog
    v-model="innerVisible"
    title=""
    top="3vh"
    width="90vw"
    destroy-on-close
    class="task-detail-dialog"
  >
    <div v-if="loading" class="loading-area">
      <el-skeleton rows="6" animated />
    </div>
    <el-empty v-else-if="!detail" description="暂无数据" />
    <div v-else class="detail-page">
      <section class="summary-card">
        <div class="summary-main">
          <p class="summary-title">{{ detail.taskName }}</p>
        </div>
        <div class="summary-right">
          <div class="summary-info-grid">
            <div class="summary-info-item">
              <span class="info-label">当前展示版本</span>
              <span class="info-value">v{{ displayedVersion?.versionNumber || detail.currentVersion }}</span>
            </div>
            <div class="summary-info-item">
              <span class="info-label">审查员</span>
              <span class="info-value">{{ detail.reviewer?.realName || '-' }}</span>
            </div>
            <div class="summary-info-item">
              <span class="info-label">发起人</span>
              <span class="info-value">{{ detail.creator?.realName || '-' }}</span>
            </div>
          </div>
          <div class="summary-actions">
            <el-tag :type="statusTag(displayedVersion?.status || detail.currentStatus)" effect="plain">
              {{ displayedVersion?.statusLabel || detail.statusLabel }}
            </el-tag>
            <el-button
              v-if="detail.canResubmit"
              type="primary"
              text
              @click="gotoResubmit"
            >
              再次提交
            </el-button>
          </div>
        </div>
      </section>

      <div class="detail-layout">
        <div class="main-content">
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <h3>版本详情</h3>
                <span class="version-label">v{{ displayedVersion?.versionNumber || '-' }}</span>
              </div>
            </template>
            <el-descriptions :column="2" border v-if="displayedVersion">
              <el-descriptions-item label="版本号">
                v{{ displayedVersion.versionNumber }}
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">
                {{ formatTime(displayedVersion.submitTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="审查时间">
                {{ formatTime(displayedVersion.reviewTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="提交说明" :span="2">
                {{ displayedVersion.submitDesc || '暂无说明' }}
              </el-descriptions-item>
              <el-descriptions-item label="审查意见" :span="2">
                {{ displayedVersion.reviewComment || '暂无意见' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <h3>版本文件</h3>
                <span class="sub-note">共 {{ fileList.length }} 个文件</span>
              </div>
            </template>
            <el-table
              :data="fileList"
              v-loading="fileLoading"
              border
              empty-text="暂无文件"
            >
              <el-table-column prop="fileName" label="文件名称" min-width="220" />
              <el-table-column prop="fileSize" label="大小" width="120">
                <template #default="{ row }">
                  {{ formatSize(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.uploadTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button type="primary" link @click="preview(row)">预览</el-button>
                  <el-button type="primary" link @click="download(row)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <aside class="side-panel">
          <div class="panel-header">
            <h3>历史版本</h3>
          </div>
          <el-scrollbar class="history-scroll">
            <div
              v-for="item in detail.versions || []"
              :key="item.versionId"
              class="history-item"
              :class="{ active: item.versionId === selectedVersionId }"
              @click="handleVersionClick(item)"
            >
              <div class="history-title">
                <span>v{{ item.versionNumber }}</span>
                <el-tag size="small" :type="statusTag(item.status)">{{ item.statusLabel }}</el-tag>
              </div>
              <p class="history-meta">
                {{ formatTime(item.submitTime) }} · {{ item.reviewerName || '未指定' }}
              </p>
            </div>
          </el-scrollbar>
        </aside>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskDetail } from '@/api/task'
import { fetchFileList, getPreviewUrl, downloadFile } from '@/api/file'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  taskId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])
const router = useRouter()

const innerVisible = computed({
  get() {
    return props.modelValue
  },
  set(val) {
    emit('update:modelValue', val)
  }
})

const detail = ref(null)
const fileList = ref([])
const loading = ref(false)
const fileLoading = ref(false)
const selectedVersionId = ref(null)
const displayedVersion = ref(null)

const loadDetail = async () => {
  loading.value = true
  detail.value = null
  try {
    const data = await getTaskDetail(props.taskId)
    detail.value = data
    const currentId = data?.currentVersionDetail?.versionId
    selectedVersionId.value = currentId
    displayedVersion.value = mapVersionDetail(data.currentVersionDetail)
    await loadFiles(currentId)
  } catch (error) {
    ElMessage.error(error?.message || '获取任务详情失败')
  } finally {
    loading.value = false
  }
}

const mapVersionDetail = (source) => {
  if (!source) return null
  return {
    versionId: source.versionId,
    versionNumber: source.versionNumber,
    fileCount: source.fileCount,
    submitTime: source.submitTime,
    reviewTime: source.reviewTime,
    submitDesc: source.submitDesc,
    reviewComment: source.reviewComment,
    status: source.status,
    statusLabel: source.statusLabel,
    reviewerName: source.reviewerName
  }
}

const handleVersionClick = async (version) => {
  if (version.versionId === selectedVersionId.value) {
    return
  }
  selectedVersionId.value = version.versionId
  displayedVersion.value = mapVersionDetail(version)
  await loadFiles(version.versionId)
}

const loadFiles = async (versionId) => {
  if (!versionId) {
    fileList.value = []
    return
  }
  fileLoading.value = true
  try {
    const data = await fetchFileList(versionId)
    fileList.value = data || []
  } catch (error) {
    ElMessage.error(error?.message || '获取文件列表失败')
  } finally {
    fileLoading.value = false
  }
}

const preview = async (row) => {
  try {
    const data = await getPreviewUrl(row.id)
    if (data?.previewUrl) {
      window.open(data.previewUrl, '_blank', 'noopener=yes')
    } else {
      ElMessage.warning('未获取到预览地址')
    }
  } catch (error) {
    ElMessage.error(error?.message || '获取预览地址失败')
  }
}

const download = async (row) => {
  try {
    const blob = await downloadFile(row.id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载失败')
  }
}

const statusTag = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

const formatSize = (size) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const gotoResubmit = () => {
  if (!detail.value?.canResubmit) return
  router.push({
    name: 'UserResubmitTask',
    params: { taskId: detail.value.taskId },
    query: { versionId: detail.value.currentVersionDetail?.versionId }
  })
}

// 弹窗展示时加载详情；组件创建时若已可见也要触发一次
watch(
  () => innerVisible.value,
  (visible) => {
    if (visible && props.taskId) {
      loadDetail()
    }
  }
)

watch(
  () => props.taskId,
  (taskId) => {
    if (taskId && innerVisible.value) {
      loadDetail()
    }
  }
)

onMounted(() => {
  if (innerVisible.value && props.taskId) {
    loadDetail()
  }
})
</script>

<style scoped>
.task-detail-dialog :deep(.el-dialog) {
  width: 100vw;
  max-width: 100vw;
}

.task-detail-dialog :deep(.el-dialog__body) {
  padding: 24px 32px 32px;
  background: #f5f7fa;
}

.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  background: linear-gradient(135deg, #f8fbff 0%, #f1f6ff 100%);
  border-radius: 14px;
  padding: 22px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e0e6f1;
  box-shadow: 0 6px 20px rgba(31, 61, 136, 0.08);
  gap: 24px;
}

.summary-main {
  flex: 1;
}

.summary-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2f3859;
}

.summary-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.summary-info-grid {
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 12px 24px;
}

.summary-info-item {
  display: flex;
  flex-direction: column;
  min-width: 120px;
}

.info-label {
  font-size: 12px;
  color: #9aa4c2;
}

.info-value {
  font-size: 16px;
  color: #2f3859;
  font-weight: 600;
}

.summary-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  min-height: 0;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.version-label {
  color: #909399;
}

.sub-note {
  color: #909399;
  font-size: 13px;
}

.side-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.history-scroll {
  max-height: 520px;
}

.history-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.history-item.active {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.08);
}

.history-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.history-meta {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
}

.loading-area {
  padding: 24px;
}
</style>
