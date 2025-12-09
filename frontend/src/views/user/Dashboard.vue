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
import { fetchMyTaskStatistics } from '@/api/task'

const loading = ref(false)
const statCards = ref([
  { title: '累计发起', value: 0, desc: '所有任务总数' },
  { title: '通过数量', value: 0, desc: '历史通过任务' },
  { title: '打回数量', value: 0, desc: '历史被打回任务' },
  { title: '本月任务', value: 0, desc: '本月已发起' }
])

const loadStats = async () => {
  loading.value = true
  try {
    const data = await fetchMyTaskStatistics()
    statCards.value = [
      { title: '累计发起', value: data.total || 0, desc: '所有任务总数' },
      { title: '通过数量', value: data.approved || 0, desc: '历史通过任务' },
      { title: '打回数量', value: data.rejected || 0, desc: '历史打回任务' },
      { title: '本月任务', value: data.monthTotal || 0, desc: '本月已发起' }
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
