<template>
  <div class="guide-page">
    <!-- 快捷导航 -->
    <div class="anchor-bar">
      <el-button type="primary" text @click="scrollTo('section1')">系统简介</el-button>
      <el-button type="success" text @click="scrollTo('section2')">角色关系</el-button>
      <el-button type="warning" text @click="scrollTo('section3')">审查流程</el-button>
      <el-button type="info" text @click="scrollTo('section4')">体验账号</el-button>
      <el-button type="danger" text @click="scrollTo('section5')">优化说明</el-button>
    </div>

    <!-- 顶部概要 -->
    <el-card shadow="hover" class="hero-card" id="section-top">
      <el-row :gutter="20" align="middle">
        <el-col :xs="24" :md="16">
          <div class="hero-text">
            <el-tag type="info" effect="dark" class="hero-badge">SaaS · 文件审查</el-tag>
            <h1>文件审查 SaaS 平台</h1>
            <p class="subtitle">
              多租户、多角色协作：提交任务、上传文件、审查流转、版本追踪，一站完成。
            </p>
            <div class="hero-tags">
              <el-tag type="primary" effect="plain">平台超管</el-tag>
              <el-tag type="success" effect="plain">企业管理员</el-tag>
              <el-tag type="warning" effect="plain">审查员</el-tag>
              <el-tag type="info" effect="plain">普通用户</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="never" class="entry-card">
            <div class="entry-title">常用入口</div>
            <div class="entry-row">
              <span class="label">前端</span>
              <a class="value link" href="http://47.100.75.96:5173/login" target="_blank" rel="noopener">登录页面</a>
            </div>
            <div class="entry-row">
              <span class="label">API 文档</span>
              <a class="value link" href="http://47.100.75.96:8080/api/doc.html" target="_blank" rel="noopener">后端api文档</a>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 1 系统能做什么 -->
    <el-card shadow="hover" class="section-card" id="section1">
      <template #header>
        <div class="card-header"><h2>1. 系统能做什么？</h2></div>
      </template>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12" v-for="item in features" :key="item.title">
          <el-card shadow="hover" class="mini-card color-card">
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 2 角色关系 -->
    <el-card shadow="hover" class="section-card" id="section2">
      <template #header>
        <div class="card-header"><h2>2. 四个角色如何协同？</h2></div>
      </template>
      <el-row :gutter="12" class="role-row">
        <el-col v-for="role in roles" :key="role.name" :xs="24" :md="6">
          <el-card shadow="hover" class="role-card">
            <div class="role-name">{{ role.name }}</div>
            <div class="role-desc">{{ role.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 3 审查流程 -->
    <el-card shadow="hover" class="section-card" id="section3">
      <template #header>
        <div class="card-header"><h2>3. 审查流程</h2></div>
      </template>
      <el-steps :active="4" align-center finish-status="success" class="flow-steps">
        <el-step title="创建任务" description="普通用户填写任务信息并发起" />
        <el-step title="上传文件" description="分片、断点续传、重复秒传" />
        <el-step title="审查员处理" description="查看文件，给出通过/驳回意见" />
        <el-step title="再次提交" description="若被驳回，补充材料再提交新版本" />
      </el-steps>
    </el-card>

    <!-- 4 体验账号 -->
    <el-card shadow="hover" class="section-card" id="section4">
      <template #header>
        <div class="card-header"><h2>4. 体验账号</h2></div>
      </template>
      <el-table :data="accounts" border style="width: 100%">
        <el-table-column prop="role" label="角色" width="160" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="password" label="密码" />
        <el-table-column prop="desc" label="说明" />
      </el-table>
    </el-card>

    <!-- 5 优化说明 -->
    <el-card shadow="hover" class="section-card" id="section5">
      <template #header>
        <div class="card-header"><h2>5. 优化说明</h2></div>
      </template>
      <el-row :gutter="16">
        <el-col v-for="opt in optimizations" :key="opt.title" :xs="24" :md="8">
          <el-card shadow="hover" class="mini-card color-card">
            <h3>{{ opt.title }}</h3>
            <p>{{ opt.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const features = ref([
  { title: '租户与账号', desc: '管理租户、首位管理员、企业用户与配额/状态。' },
  { title: '任务与版本', desc: '提交任务、上传文件、查看历史版本，驳回后可再次提交。' },
  { title: '审查流转', desc: '审查员处理分配任务，给出通过/驳回意见。' },
  { title: '文件上传', desc: '分片 MD5、断点续传、重复秒传，文件存储在 MinIO。' }
])

const roles = ref([
  { name: '平台超管', desc: '创建租户、首位企业管理员；查看平台监控。' },
  { name: '企业管理员', desc: '管理企业用户/審查員，配置用户/存储配额。' },
  { name: '审查员', desc: '查看分配任务，审查文件并给出结论。' },
  { name: '普通用户', desc: '提交任务、上传文件、查看审查结果，再次提交。' }
])

const accounts = ref([
  { role: '平台超管', username: 'admin', password: 'admin123', desc: '管理租户与平台配置' },
  { role: '企业管理员', username: 'mayun', password: 'admin123', desc: '管理企业用户、审查员' },
  { role: '审查员', username: 'checkUser1', password: 'admin123', desc: '审查任务、给出结论' },
  { role: '普通用户', username: 'user', password: 'admin123', desc: '创建任务、上传文件' }
])

const optimizations = ref([
  { title: '异步复制（RabbitMQ）', desc: '旧版本文件异步复制到新版本，前端可展示复制进度。' },
  { title: 'MD5 秒传 / 断点续传', desc: '分片 MD5 校验，重复上传直接秒传，中断后可继续。' },
  { title: '缓存与限流（Redis）', desc: '热门数据缓存，接口限流防刷，保障并发稳定。' }
])

const scrollTo = async (id) => {
  await nextTick()
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
</script>

<style scoped>
.guide-page {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  padding: 12px 12px 32px;
  background: linear-gradient(180deg, #f6f8fb 0%, #eef2f8 60%, #e8ecf4 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.anchor-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 16px;
}

.hero-card {
  width: 100%;
  border-radius: 16px;
  box-shadow: 0 14px 30px rgba(47, 56, 89, 0.08);
}

.hero-text h1 {
  margin: 10px 0 12px;
  font-size: 30px;
  color: #2f3859;
}

.hero-badge {
  margin: 0;
}

.subtitle {
  margin: 0 0 14px;
  color: #5f6581;
  line-height: 1.6;
  font-size: 15px;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.entry-card {
  border-radius: 12px;
  background: linear-gradient(135deg, #fdfefe 0%, #f3f7ff 100%);
}

.entry-title {
  font-weight: 700;
  color: #2f3859;
  margin-bottom: 10px;
}

.entry-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #3a3f55;
  margin-bottom: 6px;
}

.entry-row .label {
  color: #6b7391;
}

.entry-row .value {
  font-weight: 600;
}

.entry-tip {
  color: #909399;
  font-size: 13px;
}

.section-card {
  width: 100%;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(31, 61, 136, 0.06);
  margin: 0;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: #2f3859;
}

.feature-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
  color: #3a3f55;
  font-size: 15px;
}

.role-row {
  align-items: stretch;
}

.role-card {
  background: #fdfefe;
  border: 1px solid #e6ebf3;
  border-radius: 12px;
  min-height: 140px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(47, 56, 89, 0.08);
}

.role-name {
  font-weight: 700;
  font-size: 16px;
  color: #2f3859;
  margin-bottom: 6px;
}

.role-desc {
  color: #54607a;
  font-size: 14px;
  line-height: 1.5;
}

.mini-card {
  border-radius: 12px;
  min-height: 120px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.mini-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(47, 56, 89, 0.08);
}

.mini-card h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #2f3859;
}

.mini-card p {
  margin: 0;
  color: #54607a;
  font-size: 14px;
  line-height: 1.5;
}

.color-card:nth-child(1n) {
  background: linear-gradient(135deg, #fdfefe 0%, #f1f7ff 100%);
}

.color-card:nth-child(2n) {
  background: linear-gradient(135deg, #fffdf9 0%, #fff6ea 100%);
}

.color-card:nth-child(3n) {
  background: linear-gradient(135deg, #f9fffb 0%, #edfff4 100%);
}

.color-card:nth-child(4n) {
  background: linear-gradient(135deg, #fefbff 0%, #f7f0ff 100%);
}

.flow-steps {
  padding: 4px 6px 10px;
}

.opt-grid {
  margin-top: 6px;
}
</style>
