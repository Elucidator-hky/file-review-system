# 文件审查系统

多租户的文件审查与版本管理系统，支持平台超管、企业管理员、审查员、普通用户的分角色协同，提供文件上传、版本流转、审查记录、缓存与异步处理等能力。

> **在线体验（示例）**：`http://47.100.75.96:5173/guide

## 功能说明
- **租户与账户**：平台超管创建租户及首位管理员；企业管理员管理企业用户与配额。
- **任务与版本**：普通用户创建任务、上传文件、查看版本历史；被驳回后可再次提交。
- **审查流转**：审查员查看分配任务，给出审查结论与意见。
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
1) 启动依赖（需 Docker）  
```bash
docker-compose -f docker-compose-admin.yml up -d
```
2) 启动后端  
```bash
cd backend
mvn -DskipTests spring-boot:run   # 默认端口 8080
```
3) 启动前端  
```bash
cd frontend
npm install
npm run dev                       # 默认端口 5173
```
4) 访问  
- 前端：http://localhost:5173  
- 健康检查：`http://localhost:8080/api/health`  
- 接口文档（按实际配置）：`http://localhost:8080/doc.html` 或 `http://localhost:8080/swagger-ui/index.html`

## 示例账号（请按初始化数据补充）
| 角色       | 账号示例 | 密码示例 | 说明 |
| ---------- | -------- | -------- | ---- |
| 平台超管   | `admin` | `admin123` | 管理租户与平台配置 |
| 企业管理员 | `mayun` | `admin123` | 管理企业用户、审查员 |
| 审查员     | `checkUser1` | `admin123` | 审查任务、给出结论 |
| 普通用户   | `user` | `admin123` | 创建任务、上传文件 |

## 配置说明
- 后端配置：`backend/src/main/resources/application.yml`（数据库、Redis、RabbitMQ、MinIO、端口等）
- 前端接口地址：`frontend/vite.config.js` 或 `.env*`（如需自定义后端 API 地址）

## 常见问题
- **端口占用**：调整后端 `server.port` 或前端启动端口，或释放 8080/5173。
- **依赖未启动**：确认 Docker 中 MySQL/Redis/RabbitMQ/MinIO 容器均为 healthy。
- **大文件上传失败**：检查前端 `public/workers/md5Worker.js` 与后端 MinIO、文件大小限制。

## 部署
- 建议用 `docker-compose-admin.yml` 启动依赖；后端 `mvn package`，前端 `npm run build` 后部署。
- 生产环境请按实际域名、证书、存储/队列连接等覆盖配置。
