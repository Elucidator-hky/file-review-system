<template>
  <div class="create-task-page">
    <el-page-header content="创建审查任务" @back="goBack" />

    <el-steps :active="activeStep" align-center finish-status="success" class="step-bar">
      <el-step title="填写任务信息" />
      <el-step title="上传审查文件" />
    </el-steps>

    <div v-show="activeStep === 0" class="step-panel">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">任务信息</div>
        </template>
        <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-width="100px"
          label-position="left"
          size="large"
        >
          <el-form-item label="任务名称" prop="taskName">
            <el-input
              v-model="form.taskName"
              placeholder="请输入任务名称"
              maxlength="100"
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-form-item label="指派审查员" prop="reviewerId">
            <el-select
              v-model="form.reviewerId"
              placeholder="请选择审查员"
              filterable
              :loading="reviewerLoading"
              style="width: 100%"
            >
              <el-option
                v-for="item in reviewerOptions"
                :key="item.userId"
                :label="buildReviewerLabel(item)"
                :value="item.userId"
              >
                <div class="reviewer-option">
                  <div class="name-row">
                    <span class="name">{{ item.realName || item.username }}</span>
                    <el-tag v-if="item.dualRole" size="small" type="success">可切换前台</el-tag>
                  </div>
                  <div class="desc-row">
                    <span>账号：{{ item.username }}</span>
                    <span>手机：{{ item.phone || '-' }}</span>
                    <span>最近登录：{{ formatTime(item.lastLoginTime) }}</span>
                  </div>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="提交说明" prop="submitDesc">
            <el-input
              v-model="form.submitDesc"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 8 }"
              maxlength="2000"
              show-word-limit
              placeholder="可以简单说明任务背景、重点关注内容等"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="creating" @click="handleNext">
              下一步
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div v-show="activeStep === 1" class="step-panel">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">上传审查文件</div>
        </template>

        <el-alert
          title="任务已创建，请在同一个流程内完成文件上传。"
          type="info"
          show-icon
          class="mb-16"
        >
          <template #default>
            <div class="summary-list">
              <div>任务名称：{{ taskSummary.taskName }}</div>
              <div>指派审查员：{{ taskSummary.reviewerName }}</div>
              <div>审查员账号：{{ taskSummary.reviewerUsername }}</div>
            </div>
          </template>
        </el-alert>

        <FileUpload
          v-if="versionId"
          :version-id="versionId"
          @uploaded="handleFileChanged"
          @deleted="handleFileChanged"
        />

        <el-empty v-else description="正在获取版本信息，请稍候" />

        <div class="finish-actions">
          <div class="file-count">
            当前已上传 <span class="highlight">{{ fileCount }}</span> 个文件
          </div>
          <div class="action-buttons">
            <el-button @click="refreshFileCount" :disabled="!versionId">刷新文件数量</el-button>
            <el-button @click="goBack">返回我的任务</el-button>
            <el-button type="primary" :disabled="fileCount === 0" @click="handleFinish">
              完成并返回
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import { fetchReviewers, createTask } from '@/api/task'
import { fetchFileList } from '@/api/file'

const router = useRouter()

const activeStep = ref(0)
const formRef = ref()
const reviewerOptions = ref([])
const reviewerLoading = ref(false)
const creating = ref(false)
const fileCount = ref(0)
const taskInfo = ref(null)
const confirmedReviewer = ref(null)
const confirmedTaskName = ref('')

const form = reactive({
  taskName: '',
  reviewerId: null,
  submitDesc: ''
})

const formRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 1, max: 100, message: '任务名称不能超过100个字符', trigger: 'blur' }
  ],
  reviewerId: [{ required: true, message: '请选择审查员', trigger: 'change' }],
  submitDesc: [{ max: 2000, message: '提交说明不能超过2000个字符', trigger: 'blur' }]
}

const versionId = computed(() => taskInfo.value?.versionId || null)

const taskSummary = computed(() => ({
  taskName: confirmedTaskName.value || '-',
  reviewerName: confirmedReviewer.value?.realName || confirmedReviewer.value?.username || '-',
  reviewerUsername: confirmedReviewer.value?.username || '-'
}))

const goBack = () => {
  router.push('/user/my-tasks')
}

const handleReset = () => {
  form.taskName = ''
  form.reviewerId = null
  form.submitDesc = ''
}

const loadReviewers = async () => {
  reviewerLoading.value = true
  try {
    const list = await fetchReviewers()
    reviewerOptions.value = Array.isArray(list) ? list : []
  } catch (error) {
    reviewerOptions.value = []
  } finally {
    reviewerLoading.value = false
  }
}

const buildReviewerLabel = (item) => {
  const name = item.realName || item.username
  const phone = item.phone || '无手机号'
  const login = item.lastLoginTime ? formatTime(item.lastLoginTime) : '暂无记录'
  return `${name}（${phone}｜最近登录：${login}）`
}

const formatTime = (time) => {
  if (!time) return '暂无记录'
  return time.replace('T', ' ')
}

const handleNext = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      const response = await createTask({
        taskName: form.taskName,
        reviewerId: form.reviewerId,
        submitDesc: form.submitDesc
      })
      taskInfo.value = response
      confirmedTaskName.value = form.taskName
      confirmedReviewer.value = reviewerOptions.value.find(
        (item) => item.userId === form.reviewerId
      )
      activeStep.value = 1
      ElMessage.success('任务创建成功，请继续上传文件')
      await refreshFileCount()
    } finally {
      creating.value = false
    }
  })
}

const refreshFileCount = async () => {
  if (!versionId.value) {
    fileCount.value = 0
    return
  }
  try {
    const list = await fetchFileList(versionId.value)
    fileCount.value = Array.isArray(list) ? list.length : 0
  } catch (error) {
    fileCount.value = 0
  }
}

const handleFileChanged = () => {
  refreshFileCount()
}

const handleFinish = async () => {
  await refreshFileCount()
  if (fileCount.value === 0) {
    ElMessage.warning('请至少上传一个文件后再完成流程')
    return
  }
  ElMessage.success('任务已创建并上传文件')
  router.push('/user/my-tasks')
}

watch(versionId, () => {
  if (activeStep.value === 1) {
    refreshFileCount()
  }
})

onMounted(() => {
  loadReviewers()
})
</script>

<style scoped>
.create-task-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step-bar {
  margin: 0 0 8px;
}

.step-panel {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
}

.reviewer-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.desc-row {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  color: #606266;
}

.mb-16 {
  margin-bottom: 16px;
}

.finish-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-count {
  font-size: 14px;
  color: #606266;
}

.highlight {
  color: #409eff;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
