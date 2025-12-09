<template>
  <div class="monitor-page">
    <el-card class="summary-card" shadow="hover">
      <div class="summary-header">
        <div>
          <div class="title">Redis 缓存 - 即时指标</div>
          <div class="sub-title">最近刷新：{{ cacheData?.generatedAt || '--' }}</div>
        </div>
        <el-button type="primary" link @click="loadCache">刷新</el-button>
      </div>
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="metric-card">
            <div class="label">键数量</div>
            <div class="value">{{ cacheData?.totalKeys ?? 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="label">命中率</div>
            <div class="value">{{ hitRate }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="label">内存占用</div>
            <div class="value">{{ cacheData?.usedMemoryHuman || '0B' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="label">客户端连接</div>
            <div class="value">{{ cacheData?.connectedClients ?? 0 }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>热点缓存 Key（最多 8 条）</span>
        </div>
      </template>
      <el-table :data="cacheData?.hotKeys || []" border>
        <el-table-column prop="key" label="Key 名称" min-width="320" />
        <el-table-column label="剩余 TTL (秒)" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info" v-if="row.ttlSeconds === null || row.ttlSeconds < 0">永久</el-tag>
            <span v-else>{{ row.ttlSeconds }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCacheMetrics } from '@/api/monitor'

const cacheData = ref(null)

const loadCache = async () => {
  try {
    cacheData.value = await fetchCacheMetrics()
  } catch (error) {
    ElMessage.error('获取缓存监控数据失败')
  }
}

const hitRate = computed(() => {
  if (!cacheData.value) {
    return '0%'
  }
  return `${(cacheData.value.hitRate * 100).toFixed(2)}%`
})

onMounted(() => {
  loadCache()
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
  align-items: center;
  justify-content: space-between;
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

.stat-row {
  margin-top: 12px;
}

.metric-card {
  background: #f6f9ff;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
}

.metric-card .label {
  color: #909399;
  margin-bottom: 8px;
}

.metric-card .value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}
</style>
