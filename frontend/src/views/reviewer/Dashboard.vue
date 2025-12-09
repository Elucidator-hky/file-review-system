<template>
  <div class="dashboard-page" v-loading="loading">
    <el-row :gutter="16">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.title">
        <el-card class="stat-card">
          <p class="stat-title">{{ card.title }}</p>
          <p class="stat-value">{{ card.value }}</p>
          <p class="stat-desc">{{ card.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchReviewerTaskStatistics } from '@/api/task'

const loading = ref(false)
const statCards = ref([
  { title: '指派任务', value: 0, desc: '所有指派给我的任务' },
  { title: '待审查', value: 0, desc: '当前待处理任务' },
  { title: '已完成', value: 0, desc: '历史完成任务' },
  { title: '本月完成', value: 0, desc: '本月完成任务' }
])

const loadStats = async () => {
  loading.value = true
  try {
    const data = await fetchReviewerTaskStatistics()
    statCards.value = [
      { title: '指派任务', value: data.total || 0, desc: '所有指派给我的任务' },
      { title: '待审查', value: data.reviewing || 0, desc: '当前待处理任务' },
      { title: '已完成', value: (data.approved || 0) + (data.rejected || 0), desc: '历史完成任务' },
      { title: '本月完成', value: data.monthApproved || 0, desc: '本月完成任务' }
    ]
  } catch (error) {
    ElMessage.error(error?.message || '获取统计信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.dashboard-page {
  padding: 16px;
}

.stat-card {
  text-align: left;
}

.stat-title {
  margin: 0;
  color: #909399;
}

.stat-value {
  margin: 8px 0 4px;
  font-size: 28px;
  font-weight: 600;
}

.stat-desc {
  margin: 0;
  color: #a0a3a6;
  font-size: 12px;
}
</style>
