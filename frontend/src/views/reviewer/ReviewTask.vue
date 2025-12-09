<template>
  <div class="review-page" v-loading="loading">
    <el-page-header content="审查任务" @back="goBack" class="mb-16" />

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <h3>任务信息</h3>
              <el-tag :type="statusTag(detail?.status)">
                {{ statusLabel(detail?.status) }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border v-if="detail">
            <el-descriptions-item label="任务名称">{{ detail.taskName }}</el-descriptions-item>
            <el-descriptions-item label="版本号">v{{ detail.versionNumber }}</el-descriptions-item>
            <el-descriptions-item label="提交人">
              {{ detail.creator?.realName || '-' }}（{{ detail.creator?.phone || '暂无电话' }}）
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatTime(detail.submitTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="提交说明" :span="2">
              {{ detail.submitDesc || '无' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="mt-16" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>版本文件</h3>
              <el-tag v-if="detail?.filesReady === 0" type="warning">文件复制中</el-tag>
            </div>
          </template>
          <el-table
            :data="files"
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
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="preview(row)">预览</el-button>
                <el-button type="primary" link @click="download(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <h3>历史版本</h3>
          </template>
          <el-timeline>
            <el-timeline-item v-for="item in timeline" :key="item.title" :timestamp="formatTime(item.time)">
              <p class="timeline-title">{{ item.title }}</p>
              <p class="timeline-desc">{{ item.description }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="mt-16" shadow="never">
          <template #header>
            <h3>审查操作</h3>
          </template>
          <el-form label-position="top">
            <el-form-item label="审查意见">
              <el-input
                v-model="comment"
                type="textarea"
                :rows="6"
                maxlength="1000"
                show-word-limit
                placeholder="可选，最多 1000 字"
              />
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button type="success" :loading="submitting" :disabled="!canOperate" @click="handleApprove">
                  审查通过
                </el-button>
                <el-button type="danger" :loading="submitting" :disabled="!canOperate" @click="handleReject">
                  打回修改
                </el-button>
              </el-space>
            </el-form-item>
          </el-form>
          <p class="tips">仅状态为“审查中”且文件已就绪的版本允许操作。</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchReviewDetail, approveReview, rejectReview } from '@/api/review'
import { getPreviewUrl, downloadFile } from '@/api/file'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => Number(route.params.taskId))
const versionId = computed(() => Number(route.query.versionId))

const detail = ref(null)
const files = ref([])
const timeline = ref([])
const loading = ref(false)
const submitting = ref(false)
const comment = ref('')

const canOperate = computed(() => {
  return detail.value
    && detail.value.status === 'REVIEWING'
    && detail.value.filesReady !== 0
})

const loadDetail = async () => {
  const id = versionId.value
  if (!id) {
    ElMessage.error('缺少版本信息')
    return
  }
  loading.value = true
  try {
    const data = await fetchReviewDetail(id)
    detail.value = data
    files.value = data.files || []
    timeline.value = buildTimeline(data)
  } catch (error) {
    ElMessage.error(error?.message || '获取审查详情失败')
  } finally {
    loading.value = false
  }
}

const buildTimeline = (detail) => {
  const list = []
  if (detail.submitTime) {
    list.push({ title: '提交版本', description: detail.creator?.realName || '-', time: detail.submitTime })
  }
  if (detail.previousVersion && detail.previousVersion.reviewTime) {
    list.push({
      title: `上一版本 v${detail.previousVersion.versionNumber} 结果`,
      description: detail.previousVersion.reviewComment || '无',
      time: detail.previousVersion.reviewTime
    })
  }
  return list
}

const handleApprove = () => {
  ElMessageBox.confirm('确认通过本次审查？', '提示', {
    type: 'warning'
  }).then(async () => {
    submitting.value = true
    try {
      await approveReview(detail.value.versionId, { comment: comment.value })
      ElMessage.success('审查已通过')
      goBack()
    } catch (error) {
      ElMessage.error(error?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  }).catch(() => {})
}

const handleReject = () => {
  if (!comment.value) {
    ElMessage.warning('请填写打回原因')
    return
  }
  ElMessageBox.confirm('确认打回该版本？', '提示', {
    type: 'warning'
  }).then(async () => {
    submitting.value = true
    try {
      await rejectReview(detail.value.versionId, { comment: comment.value })
      ElMessage.success('已打回，等待提交人修改')
      goBack()
    } catch (error) {
      ElMessage.error(error?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  }).catch(() => {})
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

const statusTag = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const statusLabel = (status) => {
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已打回'
  return '审查中'
}

const goBack = () => {
  router.push({ name: 'ReviewerTasks' })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.review-page {
  padding: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.mt-16 {
  margin-top: 16px;
}

.timeline-title {
  margin: 0 0 4px;
  font-weight: 600;
}

.timeline-desc {
  margin: 0;
  color: #666;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tips {
  color: #909399;
  font-size: 12px;
}
</style>
