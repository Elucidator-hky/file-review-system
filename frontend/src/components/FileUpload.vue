<template>
  <div class="file-upload-wrapper">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
          <el-button type="primary" link @click="loadFiles" :loading="listLoading">
            刷新列表
          </el-button>
        </div>
      </template>
      <el-upload
        ref="uploadRef"
        drag
        action=""
        :auto-upload="false"
        :show-file-list="false"
        :disabled="uploading || !versionId"
        @change="handleFileChange"
      >
        <el-icon class="upload-icon">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">
          将 PDF 拖到此处，或 <em>点击上传</em>
        </div>
        <div class="el-upload__tip" v-if="!versionId">
          请先选择版本后再上传
        </div>
        <div class="el-upload__tip">仅支持 PDF，最大 200MB</div>
      </el-upload>

      <div class="progress-area" v-if="uploading">
        <div class="progress-row">
          <span>计算指纹：</span>
          <el-progress :percentage="md5Progress" :status="progressStatus(md5Progress)" />
        </div>
        <div class="progress-row">
          <span>上传进度：</span>
          <el-progress :percentage="uploadProgress" :status="progressStatus(uploadProgress)" />
        </div>
        <el-button text type="danger" @click="cancelCurrentTask">取消当前任务</el-button>
      </div>
    </el-card>

    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>已上传文件</span>
        </div>
      </template>
      <el-table
        :data="fileList"
        v-loading="listLoading"
        empty-text="暂无文件"
        border
      >
        <el-table-column prop="fileName" label="文件名称" min-width="200" />
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="preview(row)">预览</el-button>
            <el-button type="primary" link @click="download(row)">下载</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { calculateFileMd5 } from '@/utils/md5'
import {
  checkFileExists,
  uploadFile,
  fetchFileList,
  deleteFile,
  getPreviewUrl,
  downloadFile
} from '@/api/file'

const props = defineProps({
  versionId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['uploaded', 'deleted'])

const uploadRef = ref(null)
const uploading = ref(false)
const md5Progress = ref(0)
const uploadProgress = ref(0)
const listLoading = ref(false)
const fileList = ref([])
const currentTask = ref(null)

const versionId = computed(() => props.versionId)

const resetProgress = () => {
  md5Progress.value = 0
  uploadProgress.value = 0
  currentTask.value = null
}

const loadFiles = async () => {
  if (!versionId.value) {
    fileList.value = []
    return
  }
  listLoading.value = true
  try {
    const data = await fetchFileList(versionId.value)
    fileList.value = data || []
  } finally {
    listLoading.value = false
  }
}

const handleFileChange = async (uploadFile) => {
  if (!versionId.value) {
    ElMessage.warning('请先选择版本')
    return
  }
  const rawFile = uploadFile?.raw
  if (!rawFile) return
  await startUpload(rawFile)
  uploadRef.value?.clearFiles()
}

const startUpload = async (file) => {
  uploading.value = true
  try {
    const { promise, cancel } = calculateFileMd5(file, {
      chunkSize: 2 * 1024 * 1024,
      onProgress: ({ percent }) => {
        md5Progress.value = percent
      }
    })
    currentTask.value = cancel
    const md5 = await promise
    md5Progress.value = 100

    const existsResult = await checkFileExists(md5)
    if (existsResult?.exists) {
      ElMessage.info('检测到历史文件，尝试秒传...')
    }

    const formData = new FormData()
    formData.append('versionId', versionId.value)
    formData.append('fileName', file.name)
    formData.append('md5', md5)
    formData.append('file', file)

    await uploadFile(formData, {
      onUploadProgress: (event) => {
        if (event.total) {
          uploadProgress.value = Math.round((event.loaded / event.total) * 100)
        }
      }
    })
    uploadProgress.value = 100
    ElMessage.success(existsResult?.exists ? '秒传完成，已复用历史文件' : '文件上传成功')
    emit('uploaded')
    await loadFiles()
  } catch (error) {
    if (error?.message?.includes('取消')) {
      ElMessage.info('已取消当前任务')
    } else {
      ElMessage.error(error?.message || '上传失败，请稍后重试')
    }
  } finally {
    uploading.value = false
    resetProgress()
  }
}

const cancelCurrentTask = () => {
  if (currentTask.value) {
    currentTask.value()
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
    ElMessage.error(error?.message || '下载失败，请稍后重试')
  }
}

const remove = (row) => {
  ElMessageBox.confirm(`确认删除文件「${row.fileName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    emit('deleted')
    loadFiles()
  }).catch(() => {})
}

const formatSize = (size) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

const progressStatus = (percent) => {
  if (percent === 0) return 'exception'
  if (percent === 100) return 'success'
  return undefined
}

watch(versionId, () => {
  loadFiles()
})

onMounted(() => {
  loadFiles()
})

onBeforeUnmount(() => {
  cancelCurrentTask()
})
</script>

<style scoped>
.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-card,
.list-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-area {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 12px;
}
</style>
