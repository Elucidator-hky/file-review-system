<template>
  <div class="monitor-page">
    <el-card class="summary-card" shadow="hover">
      <div class="summary-header">
        <div>
          <div class="title">文件复制队列 - 实时状态</div>
          <div class="sub-title">最近刷新：{{ queueData?.generatedAt || '--' }}</div>
        </div>
        <div class="actions">
          <el-tag :type="alertTagType" effect="dark">{{ queueData?.alertMessage || '暂无数据' }}</el-tag>
          <el-button type="primary" link @click="loadQueue">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="queue-row">
      <el-col :span="12">
        <el-card class="queue-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>复制队列</span>
              <el-tag size="small" type="info">{{ queueData?.mainQueue?.statusLabel || '未知' }}</el-tag>
            </div>
          </template>
          <div class="stat-block">
            <div class="label">待处理消息</div>
            <div class="value" style="color:#ffb300">{{ queueData?.mainQueue?.ready ?? 0 }}</div>
          </div>
          <div class="stat-block">
            <div class="label">未确认消息</div>
            <div class="value" style="color:#ff6f00">{{ queueData?.mainQueue?.unacked ?? 0 }}</div>
          </div>
          <div class="stat-block">
            <div class="label">总消息数</div>
            <div class="value" style="color:#2f88ff">{{ queueData?.mainQueue?.total ?? 0 }}</div>
          </div>
          <div class="stat-block">
            <div class="label">消费者数量</div>
            <div class="value" style="color:#5cb87a">{{ queueData?.mainQueue?.consumers ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="queue-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>死信队列</span>
              <el-tag size="small" type="warning">{{ queueData?.dlxQueue?.statusLabel || '未知' }}</el-tag>
            </div>
          </template>
          <div class="stat-block">
            <div class="label">积压消息</div>
            <div class="value" style="color:#d9534f">{{ queueData?.dlxQueue?.total ?? 0 }}</div>
          </div>
          <div class="stat-block">
            <div class="label">最新写入速率</div>
            <div class="value rate" style="color:#ff9e80">
              {{ formatRate(queueData?.dlxQueue?.publishRate) }}
              <span class="unit">条/秒</span>
            </div>
          </div>
          <div class="stat-block">
            <div class="label">最近消费速率</div>
            <div class="value rate" style="color:#90caf9">
              {{ formatRate(queueData?.dlxQueue?.deliverRate) }}
              <span class="unit">条/秒</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchQueueMetrics } from '@/api/monitor'

const queueData = ref(null)

const loadQueue = async () => {
  try {
    const data = await fetchQueueMetrics()
    queueData.value = data
  } catch (error) {
    ElMessage.error('获取队列监控数据失败')
  }
}

const alertTagType = computed(() => {
  if (!queueData.value) {
    return 'info'
  }
  if (queueData.value.alertLevel === 'danger') {
    return 'danger'
  }
  if (queueData.value.alertLevel === 'warning') {
    return 'warning'
  }
  return 'success'
})

const formatRate = (value) => {
  if (value === null || value === undefined) {
    return 0
  }
  return Number(value.toFixed(2))
}

onMounted(() => {
  loadQueue()
})
</script>

<style scoped>
.monitor-page {
  padding: 16px;
}

.summary-card {
  margin-bottom: 16px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.sub-title {
  color: #909399;
  margin-top: 4px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.queue-row {
  margin-top: 8px;
}

.queue-card {
  min-height: 280px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-block {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
}

.stat-block:last-child {
  border-bottom: none;
}

.stat-block .label {
  color: #909399;
}

.stat-block .value {
  font-size: 24px;
  font-weight: 600;
}

.stat-block .unit {
  font-size: 14px;
  margin-left: 4px;
  color: #909399;
}
</style>
