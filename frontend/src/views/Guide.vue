<template>
  <div class="guide-page">
    <!-- Section 1: 顶部欢迎区 Hero -->
    <section class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">文件审查管理平台</h1>
        <p class="hero-subtitle">提交文件 · 在线审批 · 追踪进度</p>
        <p class="hero-desc">让企业文件审查流程更规范、更高效</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="goLogin">
            立即登录
          </el-button>
          <el-button size="large" text @click="scrollTo('platform-intro')">
            了解更多
            <el-icon class="down-icon"><ArrowDown /></el-icon>
          </el-button>
        </div>
      </div>
    </section>

    <!-- Section 2: 平台介绍 -->
    <section id="platform-intro" class="section platform-section">
      <div class="section-container">
        <div class="platform-content">
          <div class="platform-text">
            <h2 class="section-title">这是什么平台？</h2>
            <p class="platform-intro">
              一个面向<strong>多家企业</strong>的文件审查 SaaS 平台。
            </p>
            <ul class="feature-list">
              <li>
                <el-icon class="check-icon"><Check /></el-icon>
                <span>每家企业拥有独立空间</span>
              </li>
              <li>
                <el-icon class="check-icon"><Check /></el-icon>
                <span>企业间数据完全隔离</span>
              </li>
              <li>
                <el-icon class="check-icon"><Check /></el-icon>
                <span>各企业独立管理员工</span>
              </li>
              <li>
                <el-icon class="check-icon"><Check /></el-icon>
                <span>平台统一分配存储额度</span>
              </li>
            </ul>
            <div class="use-cases">
              <span class="label">适用场景：</span>
              <el-tag type="info" effect="plain">合同审批</el-tag>
              <el-tag type="info" effect="plain">报告审核</el-tag>
              <el-tag type="info" effect="plain">文档归档</el-tag>
              <el-tag type="info" effect="plain">资料审查</el-tag>
            </div>
          </div>
          <div class="platform-diagram">
            <div class="diagram-wrapper">
              <!-- 平台管理员 -->
              <div class="diagram-level level-top">
                <div class="diagram-node platform-admin">
                  <el-icon><Setting /></el-icon>
                  <span>平台管理员</span>
                </div>
              </div>
              <div class="diagram-arrow">
                <span class="arrow-text">创建企业 · 分配额度</span>
              </div>
              <!-- 企业层 -->
              <div class="diagram-level level-bottom">
                <div class="diagram-node enterprise" v-for="i in 3" :key="i">
                  <div class="enterprise-header">
                    <el-icon><OfficeBuilding /></el-icon>
                    <span>企业 {{ ['A', 'B', 'C'][i-1] }}</span>
                  </div>
                  <div class="enterprise-roles">
                    <div class="role-item">
                      <el-icon><User /></el-icon>
                      <span>管理员</span>
                    </div>
                    <div class="role-item">
                      <el-icon><View /></el-icon>
                      <span>审查员</span>
                    </div>
                    <div class="role-item">
                      <el-icon><UserFilled /></el-icon>
                      <span>用户</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 3: 功能场景 -->
    <section class="section features-section">
      <div class="section-container">
        <h2 class="section-title center">这个系统能帮你做什么？</h2>
        <div class="features-grid">
          <div class="feature-card" v-for="(item, index) in features" :key="index">
            <div class="feature-icon" :style="{ background: item.bgColor }">
              <el-icon :size="32" :color="item.iconColor">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <h3 class="feature-title">{{ item.title }}</h3>
            <p class="feature-desc">{{ item.desc }}</p>
            <p class="feature-detail">{{ item.detail }}</p>
            <div class="feature-role">
              <el-tag :type="item.tagType" size="small" effect="plain">
                适用角色: {{ item.role }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 4: 审查流程 -->
    <section class="section flow-section">
      <div class="section-container">
        <h2 class="section-title center">一个完整的审查流程是怎样的？</h2>

        <!-- 主流程 -->
        <div class="flow-main">
          <div class="flow-step" v-for="(step, index) in mainFlow" :key="index">
            <div class="step-icon" :style="{ background: step.bgColor }">
              <el-icon :size="28" color="#fff">
                <component :is="step.icon" />
              </el-icon>
            </div>
            <div class="step-number">{{ index + 1 }}</div>
            <h4 class="step-title">{{ step.title }}</h4>
            <ul class="step-points">
              <li v-for="(point, i) in step.points" :key="i">{{ point }}</li>
            </ul>
            <div class="step-arrow" v-if="index < mainFlow.length - 1">
              <el-icon :size="24" color="#dcdfe6"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <!-- 打回分支 -->
        <div class="flow-branch">
          <div class="branch-header">
            <el-icon color="#e6a23c"><WarningFilled /></el-icon>
            <span>如果被打回怎么办？</span>
          </div>
          <div class="branch-steps">
            <div class="branch-step" v-for="(step, index) in rejectFlow" :key="index">
              <div class="branch-icon">
                <el-icon :size="20">
                  <component :is="step.icon" />
                </el-icon>
              </div>
              <div class="branch-text">
                <h5>{{ step.title }}</h5>
                <p>{{ step.desc }}</p>
              </div>
              <el-icon v-if="index < rejectFlow.length - 1" class="branch-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <div class="branch-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>系统会保留所有历史版本，你可以随时查看之前提交的内容</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 5: 角色入口 -->
    <section class="section roles-section">
      <div class="section-container">
        <h2 class="section-title center">我是哪个角色？</h2>
        <p class="section-subtitle">根据你的职责，选择对应的入口</p>

        <div class="roles-grid">
          <div
            class="role-card"
            v-for="(role, index) in roles"
            :key="index"
            :class="{ active: activeRole === index }"
            @mouseenter="activeRole = index"
            @mouseleave="activeRole = -1"
          >
            <div class="role-icon" :style="{ background: role.bgColor }">
              <el-icon :size="36" :color="role.iconColor">
                <component :is="role.icon" />
              </el-icon>
            </div>
            <h3 class="role-name">{{ role.name }}</h3>
            <p class="role-desc">{{ role.desc }}</p>
            <div class="role-divider"></div>
            <ul class="role-abilities">
              <li v-for="(ability, i) in role.abilities" :key="i">
                <el-icon class="ability-icon"><CircleCheck /></el-icon>
                <span>{{ ability }}</span>
              </li>
            </ul>
            <div class="role-account">
              <div class="account-row">
                <span class="account-label">账号</span>
                <span class="account-value">{{ role.account }}</span>
                <el-button size="small" text @click.stop="copyText(role.account)">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </div>
              <div class="account-row">
                <span class="account-label">密码</span>
                <span class="account-value">admin123</span>
                <el-button size="small" text @click.stop="copyText('admin123')">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </div>
            </div>
            <el-button
              :type="role.btnType"
              class="role-btn"
              @click="goLogin"
            >
              去登录
            </el-button>
          </div>
        </div>

        <div class="roles-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>如果你同时是"普通用户"和"审查员"，登录后可以在右上角切换角色</span>
        </div>
      </div>
    </section>

    <!-- Section 7: 技术亮点 -->
    <section class="section tech-section">
      <div class="section-container">
        <h2 class="section-title center">技术亮点</h2>
        <p class="section-subtitle">我们在这些方面做了优化，让你使用更流畅</p>

        <div class="tech-grid">
          <div class="tech-card" v-for="(tech, index) in techFeatures" :key="index">
            <div class="tech-icon" :style="{ background: tech.bgColor }">
              <el-icon :size="28" :color="tech.iconColor">
                <component :is="tech.icon" />
              </el-icon>
            </div>
            <h4 class="tech-title">{{ tech.title }}</h4>
            <p class="tech-desc">{{ tech.desc }}</p>
            <ul class="tech-points">
              <li v-for="(point, i) in tech.points" :key="i">
                <el-icon class="point-icon" color="#67c23a"><Select /></el-icon>
                <span>{{ point }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 8: 页脚 -->
    <footer class="footer-section">
      <div class="footer-content">
        <div class="footer-links">
          <a href="#" @click.prevent>文件审查管理平台</a>
          <el-divider direction="vertical" />
          <a href="#" @click.prevent>技术支持</a>
          <el-divider direction="vertical" />
          <a :href="apiDocUrl" target="_blank">API 文档</a>
        </div>
        <p class="footer-copyright">© 2025 All Rights Reserved</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document,
  DocumentCopy,
  ArrowDown,
  ArrowRight,
  Check,
  Setting,
  OfficeBuilding,
  User,
  View,
  UserFilled,
  CircleCheck,
  InfoFilled,
  WarningFilled,
  Select,
  Edit,
  Search,
  Management,
  Clock,
  CircleClose,
  Refresh,
  DocumentChecked,
  Upload,
  Lightning,
  Box,
  Lock,
  Connection,
  Cpu,
  Histogram
} from '@element-plus/icons-vue'

import { ElMessage } from 'element-plus'

const router = useRouter()
const activeRole = ref(-1)

// API 文档地址
const apiDocUrl = `${window.location.protocol}//${window.location.hostname}:8080/api/doc.html`

// 功能场景数据
const features = ref([
  {
    icon: shallowRef(Upload),
    title: '提交文件等待审批',
    desc: '把合同、报告等文件提交给审查员，等待审批结果。',
    detail: '如果被打回，可以根据意见修改后再次提交新版本。',
    role: '普通用户',
    bgColor: 'linear-gradient(135deg, #e8f4fd 0%, #d4e8fc 100%)',
    iconColor: '#409eff',
    tagType: 'primary'
  },
  {
    icon: shallowRef(Search),
    title: '审查别人的文件',
    desc: '查看分配给你的审查任务，在线预览文件。',
    detail: '可以填写审查意见，给出"通过"或"打回"的结论。',
    role: '审查员',
    bgColor: 'linear-gradient(135deg, #e8f8e8 0%, #d4f0d4 100%)',
    iconColor: '#67c23a',
    tagType: 'success'
  },
  {
    icon: shallowRef(Management),
    title: '管理企业账号',
    desc: '创建员工账号，分配角色权限。',
    detail: '查看企业数据统计报表，监控存储空间和用户数量。',
    role: '企业管理员',
    bgColor: 'linear-gradient(135deg, #fdf6e8 0%, #fcecd4 100%)',
    iconColor: '#e6a23c',
    tagType: 'warning'
  }
])

// 主流程数据
const mainFlow = ref([
  {
    icon: shallowRef(Edit),
    title: '创建任务',
    points: ['填写任务名称', '选择审查员', '填写说明(可选)'],
    bgColor: 'linear-gradient(135deg, #409eff 0%, #66b1ff 100%)'
  },
  {
    icon: shallowRef(Upload),
    title: '上传文件',
    points: ['上传 PDF 文件', '支持多个文件', '大文件自动分片'],
    bgColor: 'linear-gradient(135deg, #67c23a 0%, #85ce61 100%)'
  },
  {
    icon: shallowRef(Clock),
    title: '等待审查',
    points: ['审查员收到通知', '在线查看文件', '给出审查结论'],
    bgColor: 'linear-gradient(135deg, #e6a23c 0%, #ebb563 100%)'
  },
  {
    icon: shallowRef(DocumentChecked),
    title: '审查完成',
    points: ['查看审查结果', '通过或被打回', '查看审查意见'],
    bgColor: 'linear-gradient(135deg, #909399 0%, #a6a9ad 100%)'
  }
])

// 打回流程数据
const rejectFlow = ref([
  {
    icon: shallowRef(CircleClose),
    title: '审查打回',
    desc: '审查员给出打回结论'
  },
  {
    icon: shallowRef(View),
    title: '查看意见',
    desc: '查看审查员的打回原因'
  },
  {
    icon: shallowRef(Edit),
    title: '修改文件',
    desc: '根据意见修改或替换文件'
  },
  {
    icon: shallowRef(Refresh),
    title: '再次提交',
    desc: '提交新版本(v2)继续审查'
  }
])

// 角色数据
const roles = ref([
  {
    icon: shallowRef(UserFilled),
    name: '普通用户',
    desc: '我需要提交文件给别人审批',
    abilities: ['创建审查任务', '上传审查文件', '查看审查结果'],
    btnText: '去提交任务',
    btnType: 'primary',
    bgColor: 'linear-gradient(135deg, #e8f4fd 0%, #d4e8fc 100%)',
    iconColor: '#409eff',
    account: 'user'
  },
  {
    icon: shallowRef(Search),
    name: '审查员',
    desc: '我负责审查别人提交的文件',
    abilities: ['查看审查任务', '在线预览文件', '给出审查结论'],
    btnText: '去审查任务',
    btnType: 'success',
    bgColor: 'linear-gradient(135deg, #e8f8e8 0%, #d4f0d4 100%)',
    iconColor: '#67c23a',
    account: 'checkUser1'
  },
  {
    icon: shallowRef(Management),
    name: '企业管理员',
    desc: '我管理企业内部的员工账号',
    abilities: ['创建员工账号', '分配角色权限', '查看数据统计'],
    btnText: '去管理后台',
    btnType: 'warning',
    bgColor: 'linear-gradient(135deg, #fdf6e8 0%, #fcecd4 100%)',
    iconColor: '#e6a23c',
    account: 'mayun'
  },
  {
    icon: shallowRef(Setting),
    name: '平台管理员',
    desc: '我管理整个平台的企业租户',
    abilities: ['创建企业租户', '分配存储额度', '监控平台数据'],
    btnText: '去平台后台',
    btnType: 'info',
    bgColor: 'linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%)',
    iconColor: '#909399',
    account: 'admin'
  }
])

// 技术亮点数据
const techFeatures = ref([
  {
    icon: shallowRef(Lightning),
    title: '大文件快速上传',
    desc: '上传大文件时，系统会自动将文件切分成小块分批传输。',
    points: ['支持 500MB 大文件', '上传中断可继续', '实时显示进度'],
    bgColor: 'linear-gradient(135deg, #fef0e8 0%, #fde2d4 100%)',
    iconColor: '#f56c6c'
  },
  {
    icon: shallowRef(Box),
    title: '智能秒传',
    desc: '如果你上传的文件之前已经上传过，系统会自动识别并秒速完成。',
    points: ['相同文件秒速完成', '节省上传时间', '自动识别重复'],
    bgColor: 'linear-gradient(135deg, #e8f4fd 0%, #d4e8fc 100%)',
    iconColor: '#409eff'
  },
  {
    icon: shallowRef(Lock),
    title: '数据安全隔离',
    desc: '每家企业的数据完全独立存储，其他企业无法查看你的文件。',
    points: ['企业间数据隔离', '文件加密存储', '权限严格管控'],
    bgColor: 'linear-gradient(135deg, #e8f8e8 0%, #d4f0d4 100%)',
    iconColor: '#67c23a'
  },
  {
    icon: shallowRef(Connection),
    title: '文件智能复用',
    desc: '再次提交时，如果文件内容没有变化，系统会自动复用已有文件。',
    points: ['相同文件自动复用', '节省存储空间', '提交速度更快'],
    bgColor: 'linear-gradient(135deg, #f0e8fd 0%, #e2d4fc 100%)',
    iconColor: '#9b59b6'
  },
  {
    icon: shallowRef(Cpu),
    title: '高速缓存',
    desc: '常用数据会缓存在内存中，打开页面和查询列表更快响应。',
    points: ['列表加载更快', '减少等待时间', '智能更新数据'],
    bgColor: 'linear-gradient(135deg, #fdf6e8 0%, #fcecd4 100%)',
    iconColor: '#e6a23c'
  },
  {
    icon: shallowRef(Histogram),
    title: '接口限流保护',
    desc: '系统会自动控制请求频率，防止恶意刷接口，保障服务稳定。',
    points: ['防止系统过载', '保障高并发稳定', '公平使用资源'],
    bgColor: 'linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%)',
    iconColor: '#909399'
  }
])

// 方法
const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const goLogin = () => {
  // 打开新页面跳转到登录页
  window.open('/login', '_blank')
}

const copyText = async (text) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      // 降级方案：使用 execCommand
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed'
      textArea.style.left = '-9999px'
      document.body.appendChild(textArea)
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
    }
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
/* 全局样式 */
.guide-page {
  min-height: 100vh;
  background: #fff;
  overflow-x: hidden;
}

/* Hero 区域 */
.hero-section {
  position: relative;
  min-height: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e8f4fd 0%, #f0f7ff 50%, #e8f0f8 100%);
}

.hero-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -30%;
  width: 80%;
  height: 150%;
  background: radial-gradient(ellipse, rgba(64, 158, 255, 0.08) 0%, transparent 70%);
}

.hero-content {
  position: relative;
  text-align: center;
  padding: 60px 24px;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: #2f3859;
  margin: 0 0 16px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 24px;
  color: #409eff;
  margin: 0 0 12px;
  font-weight: 500;
}

.hero-desc {
  font-size: 18px;
  color: #5f6581;
  margin: 0 0 40px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero-actions .el-button--large {
  padding: 12px 32px;
  font-size: 16px;
}

.down-icon {
  margin-left: 4px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(6px); }
  60% { transform: translateY(3px); }
}

/* 通用 Section 样式 */
.section {
  padding: 80px 24px;
}

.section-container {
  width: 100%;
  padding: 0 48px;
  box-sizing: border-box;
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 16px;
}

.section-title.center {
  text-align: center;
}

.section-subtitle {
  text-align: center;
  color: #5f6581;
  font-size: 16px;
  margin: 0 0 48px;
}

/* 平台介绍 */
.platform-section {
  background: #fff;
}

.platform-content {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 60px;
  align-items: center;
}

.platform-intro {
  font-size: 18px;
  color: #5f6581;
  margin: 0 0 24px;
  line-height: 1.8;
}

.platform-intro strong {
  color: #409eff;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  font-size: 16px;
  color: #3a3f55;
}

.check-icon {
  color: #67c23a;
  font-size: 20px;
}

.use-cases {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.use-cases .label {
  color: #909399;
  font-size: 14px;
}

/* 平台图示 */
.diagram-wrapper {
  background: linear-gradient(135deg, #f8fafc 0%, #f0f4f8 100%);
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(47, 56, 89, 0.08);
}

.diagram-level {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.diagram-node {
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
  color: #2f3859;
}

.diagram-node.platform-admin {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
}

.diagram-arrow {
  text-align: center;
  padding: 16px 0;
  color: #909399;
  font-size: 13px;
}

.diagram-arrow::before {
  content: '';
  display: block;
  width: 2px;
  height: 24px;
  background: linear-gradient(to bottom, #409eff, #dcdfe6);
  margin: 0 auto 8px;
}

.diagram-node.enterprise {
  flex-direction: column;
  padding: 16px;
  min-width: 140px;
}

.enterprise-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #2f3859;
}

.enterprise-roles {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #5f6581;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 6px;
}

/* 功能场景 */
.features-section {
  background: #f8fafc;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 4px 20px rgba(47, 56, 89, 0.06);
  transition: all 0.3s ease;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(47, 56, 89, 0.12);
}

.feature-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.feature-title {
  font-size: 20px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 12px;
}

.feature-desc {
  font-size: 15px;
  color: #5f6581;
  margin: 0 0 8px;
  line-height: 1.6;
}

.feature-detail {
  font-size: 14px;
  color: #909399;
  margin: 0 0 16px;
  line-height: 1.6;
}

.feature-role {
  margin-top: auto;
}

/* 审查流程 */
.flow-section {
  background: #fff;
}

.flow-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 48px;
  position: relative;
}

.flow-step {
  flex: 1;
  text-align: center;
  position: relative;
  padding: 0 16px;
}

.step-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.step-number {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(24px);
  width: 24px;
  height: 24px;
  background: #fff;
  border: 2px solid #409eff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 12px;
}

.step-points {
  list-style: none;
  padding: 0;
  margin: 0;
}

.step-points li {
  font-size: 14px;
  color: #5f6581;
  padding: 4px 0;
}

.step-arrow {
  position: absolute;
  top: 36px;
  right: -12px;
  z-index: 1;
}

/* 打回分支 */
.flow-branch {
  background: linear-gradient(135deg, #fffbf0 0%, #fff8e6 100%);
  border-radius: 16px;
  padding: 32px;
  border: 1px solid #faecd8;
}

.branch-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 24px;
}

.branch-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.branch-step {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.branch-icon {
  width: 44px;
  height: 44px;
  background: #fff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e6a23c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.branch-text h5 {
  font-size: 15px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 4px;
}

.branch-text p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.branch-arrow {
  color: #dcdfe6;
  flex-shrink: 0;
}

.branch-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #faecd8;
  font-size: 14px;
  color: #909399;
  font-style: italic;
}

/* 角色入口 */
.roles-section {
  background: #f8fafc;
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.role-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 20px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(47, 56, 89, 0.06);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.role-card:hover,
.role-card.active {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(47, 56, 89, 0.12);
}

.role-card:nth-child(1):hover,
.role-card:nth-child(1).active {
  border-color: #409eff;
}

.role-card:nth-child(2):hover,
.role-card:nth-child(2).active {
  border-color: #67c23a;
}

.role-card:nth-child(3):hover,
.role-card:nth-child(3).active {
  border-color: #e6a23c;
}

.role-card:nth-child(4):hover,
.role-card:nth-child(4).active {
  border-color: #909399;
}

.role-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.role-name {
  font-size: 20px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 8px;
}

.role-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 16px;
  min-height: 40px;
}

.role-divider {
  height: 1px;
  background: #ebeef5;
  margin: 0 -20px 16px;
}

.role-abilities {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  text-align: left;
}

.role-abilities li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
  color: #5f6581;
}

.ability-icon {
  color: #67c23a;
  font-size: 16px;
}

.role-btn {
  width: 100%;
}

.role-account {
  font-size: 13px;
  color: #5f6581;
  background: #f5f7fa;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.account-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.account-label {
  color: #909399;
  width: 32px;
}

.account-value {
  flex: 1;
  font-family: monospace;
  color: #2f3859;
}

.roles-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #909399;
}

/* 技术亮点 */
.tech-section {
  background: linear-gradient(135deg, #f0f7ff 0%, #f8fafc 100%);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.tech-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 4px 20px rgba(47, 56, 89, 0.06);
  transition: all 0.3s ease;
}

.tech-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(47, 56, 89, 0.1);
}

.tech-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.tech-title {
  font-size: 18px;
  font-weight: 600;
  color: #2f3859;
  margin: 0 0 10px;
}

.tech-desc {
  font-size: 14px;
  color: #5f6581;
  margin: 0 0 16px;
  line-height: 1.6;
}

.tech-points {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tech-points li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 13px;
  color: #5f6581;
}

.point-icon {
  font-size: 14px;
}

/* 页脚 */
.footer-section {
  background: #2f3859;
  padding: 32px 24px;
}

.footer-content {
  width: 100%;
  padding: 0 48px;
  box-sizing: border-box;
  text-align: center;
}

.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.footer-links a {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #fff;
}

.footer-links .el-divider {
  border-color: rgba(255, 255, 255, 0.2);
}

.footer-copyright {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin: 0;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .roles-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .tech-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 992px) {
  .platform-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .flow-main {
    flex-direction: column;
    align-items: center;
    gap: 32px;
  }

  .flow-step {
    width: 100%;
    max-width: 300px;
  }

  .step-arrow {
    position: static;
    transform: rotate(90deg);
    margin: 16px 0;
  }

  .branch-steps {
    flex-direction: column;
    gap: 20px;
  }

  .branch-step {
    width: 100%;
  }

  .branch-arrow {
    transform: rotate(90deg);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 18px;
  }

  .section {
    padding: 48px 16px;
  }

  .section-title {
    font-size: 24px;
  }

  .features-grid,
  .roles-grid,
  .tech-grid {
    grid-template-columns: 1fr;
  }

  .diagram-level.level-bottom {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
