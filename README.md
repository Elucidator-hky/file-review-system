# 文件审查系统

多租户的文件审查与版本管理系统，支持平台超管、企业管理员、审查员、普通用户的分角色协同，提供文件上传、版本流转、审查记录、缓存与异步处理等能力。

## 功能特性
- **租户与账户**：平台超管管理租户及首位管理员；企业管理员管理企业用户与配额。
- **任务与版本**：普通用户创建任务、上传文件、查看版本历史；被驳回后可再次提交。
- **审查流转**：审查员查看分配任务、给出审查结论与意见。
- **文件能力**：MinIO 存储，分片 MD5 校验、断点续传、重复上传秒传。
- **性能与可观测**：Redis 缓存、限流；RabbitMQ 异步复制旧版本文件；缓存/队列监控面板。

## 技术栈
- 后端：Spring Boot 2.7、MyBatis-Plus、Redis、RabbitMQ、MinIO、Knife4j/Swagger
- 前端：Vite + Vue3 + Element Plus + Pinia + Vue Router
- 数据库：MySQL

## 目录结构
```
backend/    # Spring Boot 后端
frontend/   # Vite + Vue 前端
docker-compose-admin.yml  # 一键启动基础依赖（MySQL/Redis/RabbitMQ/MinIO 等）
```

## 快速开始
### 1) 启动依赖服务
确保已安装 Docker & Docker Compose：
```bash
docker-compose -f docker-compose-admin.yml up -d
```

### 2) 启动后端
```bash
cd backend
mvn -DskipTests spring-boot:run
# 默认端口：8080
```

### 3) 启动前端
```bash
cd frontend
npm install
npm run dev
# 默认端口：5173
```

### 4) 访问
- 前端：http://localhost:5173
- 后端健康检查（示例）：http://localhost:8080/api/health
- 接口文档（按实际配置二选一）：http://localhost:8080/doc.html 或 http://localhost:8080/swagger-ui/index.html

## 环境与配置
- 后端主要配置：`backend/src/main/resources/application.yml`
  - 数据库、Redis、RabbitMQ、MinIO 地址与账号
  - 端口可通过 `server.port` 调整
- 前端环境变量：如需自定义接口地址，检查 `frontend/vite.config.js` 或 `.env` 文件

## 角色与示例账号
请根据初始化数据补充实际账号与初始密码：
- 平台超管：`<填入>`（管理租户与平台配置）
- 企业管理员：`<填入>`（管理企业用户与审查员）
- 审查员：`<填入>`
- 普通用户：`<填入>`

## 常见问题
- **端口被占用**：修改后端 `server.port` 或前端启动参数，或释放占用的 8080/5173。
- **MinIO/RabbitMQ/Redis 未启动**：先执行 `docker-compose -f docker-compose-admin.yml up -d`，确认容器健康状态。
- **大文件上传失败**：检查前端 `public/workers/md5Worker.js` 与后端 MinIO/文件大小限制配置。

## 部署
- 推荐使用 `docker-compose-admin.yml` 启动依赖，再将后端打包（`mvn package`）与前端构建产物（`npm run build`）部署到生产环境。
- 如需正式部署脚本或环境变量模板，请结合实际环境补充。

