# 开发变更日志

## 2025-11-28 - 阶段一完成

### 环境与基础架构 ✅

#### 1.1 Docker环境配置
- ✅ 创建 `docker-compose.yml`：MySQL 8.0、Redis 7.0、MinIO、RabbitMQ 3.12
- ✅ 创建 `docker-compose-admin.yml`：Adminer (MySQL)、Redis Commander
- ✅ 创建 `.env` 环境变量配置
- ✅ 创建 `README-Docker.md` 使用文档

#### 1.2 数据库初始化
- ✅ 创建 `sql/schema.sql`：5张表（tenant、user、review_task、review_version、review_file）
- ✅ 创建 `sql/init-data.sql`：测试数据（1个平台管理员 + 2个测试租户 + 9个测试用户）
- ✅ 执行 SQL 脚本，数据库初始化成功

#### 1.3 后端项目骨架
- ✅ 创建 Maven 项目结构
- ✅ 配置 `pom.xml`：Spring Boot 2.7.18、MyBatis-Plus 3.5.5、Knife4j 4.1.0
- ✅ 配置 `application.yml`：数据库、Redis、MinIO、RabbitMQ
- ✅ 创建启动类、通用响应类、全局异常处理
- ✅ 创建配置类：MyBatis-Plus、CORS、Knife4j
- ✅ 创建健康检查接口
- ✅ Maven 编译成功
- ✅ Maven 添加到系统环境变量

#### 1.4 前端项目骨架
- ✅ 使用 Vite 创建 Vue 3 项目
- ✅ 安装依赖：Element Plus、Axios、Pinia、Vue Router
- ✅ 配置 `vite.config.js`：开发服务器（端口5173）、API代理
- ✅ 创建目录结构：api、router、store、views、components、utils
- ✅ 创建 Axios 封装（request.js）
- ✅ 创建 Router 配置
- ✅ 创建 Pinia Store（用户状态管理）
- ✅ 创建 Home 页面
- ✅ 前端启动成功：http://localhost:5173

### 技术决策

#### API 文档工具
- **原计划**：Swagger
- **实际采用**：Knife4j 4.1.0 (`knife4j-openapi2-spring-boot-starter`)
- **原因**：Knife4j 是 Swagger 增强版，提供更美观的中文界面

#### 数据库连接池
- **原计划**：Druid
- **实际采用**：HikariCP（Spring Boot 默认）
- **原因**：HikariCP 性能更优，配置简单

#### RabbitMQ
- **原计划**：标注为"可选"
- **实际采用**：已在 Docker 中部署
- **原因**：直接使用消息队列处理异步任务，避免后期改造

#### 前端设备支持
- **决策**：PC端优先，移动端可访问但不做特殊优化
- **原因**：文件审查系统主要办公场景使用，PC端效率更高

### 文档更新

#### 技术文档.md
1. 更新后端技术栈：
   - Spring Boot 版本：2.7.x → 2.7.18
   - MyBatis-Plus 版本：3.5.x → 3.5.5
   - 数据库连接池：Druid → HikariCP
   - 新增：Knife4j 4.1.0
   - 新增：JWT 0.11.5
   - 新增：Hutool 5.8.25
   - RabbitMQ：标注"可选" → 3.12（已部署）

2. 更新前端技术栈：
   - 新增设备支持说明（PC端优先）

3. 更新 API 文档说明：
   - Swagger → Knife4j 4.1.0
   - 新增访问地址：http://localhost:8080/api/doc.html

4. 更新架构特点：
   - 新增"PC端优先"说明
   - 异步处理：@Async（可选RabbitMQ）→ RabbitMQ

### 下一步计划
- 阶段二：用户认证与授权
