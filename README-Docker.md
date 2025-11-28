# Docker 环境使用说明

## 服务列表

本项目使用 Docker Compose 编排以下服务：

| 服务 | 端口 | 用户名 | 密码 | 说明 |
|------|------|--------|------|------|
| MySQL 8.0 | 3306 | root | root123456 | 关系型数据库 |
| Redis 7.0 | 6379 | - | redis123456 | 缓存数据库 |
| MinIO | 9000/9001 | minioadmin | minioadmin123 | 对象存储 |
| RabbitMQ | 5672/15672 | admin | admin123456 | 消息队列 |

## 快速启动

### 1. 启动所有服务
```bash
docker-compose up -d
```

### 2. 查看服务状态
```bash
docker-compose ps
```

预期输出（所有服务都是 `running` 或 `healthy` 状态）：
```
NAME                    STATUS              PORTS
fileReview-mysql        Up (healthy)        0.0.0.0:3306->3306/tcp
fileReview-redis        Up (healthy)        0.0.0.0:6379->6379/tcp
fileReview-minio        Up (healthy)        0.0.0.0:9000-9001->9000-9001/tcp
fileReview-rabbitmq     Up (healthy)        0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
```

### 3. 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看指定服务日志
docker-compose logs -f mysql
docker-compose logs -f redis
docker-compose logs -f minio
docker-compose logs -f rabbitmq
```

## 访问管理界面

### MinIO 控制台
- 地址：http://localhost:9001
- 用户名：minioadmin
- 密码：minioadmin123

### RabbitMQ 管理界面
- 地址：http://localhost:15672
- 用户名：admin
- 密码：admin123456

## 常用命令

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 重启单个服务
```bash
docker-compose restart mysql
```

### 删除所有数据（谨慎使用！）
```bash
docker-compose down -v
```

## 数据持久化

以下数据卷用于持久化存储：
- `mysql-data`：MySQL 数据库文件
- `redis-data`：Redis 持久化数据
- `minio-data`：MinIO 对象存储数据
- `rabbitmq-data`：RabbitMQ 消息数据

## 连接信息（供后端配置使用）

### MySQL
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/file_review_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root123456
```

### Redis
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: redis123456
```

### MinIO
```yaml
minio:
  endpoint: http://localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin123
  bucketName: review-files
```

### RabbitMQ
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123456
```

## 故障排查

### 端口冲突
如果启动时提示端口被占用，可以修改 `.env` 文件中的端口配置。

### 服务无法启动
```bash
# 查看具体错误日志
docker-compose logs [服务名]

# 重新拉取镜像
docker-compose pull

# 删除容器后重新创建
docker-compose down
docker-compose up -d
```

### 健康检查失败
等待1-2分钟，服务启动需要时间。如果持续失败，查看日志排查问题。

## 注意事项

1. **首次启动**：服务启动需要1-2分钟，请耐心等待所有服务变为 `healthy` 状态
2. **数据库初始化**：MySQL 启动后会自动执行 `sql/` 目录下的 SQL 脚本
3. **网络问题**：如果镜像下载慢，可以配置 Docker 镜像加速
4. **生产环境**：请修改所有默认密码！

## 开发环境建议

- 开发时保持 Docker 服务运行
- 后端和前端在本地运行（不放入 Docker）
- 仅基础服务（MySQL、Redis、MinIO、RabbitMQ）使用 Docker

## 生产环境部署

生产环境部署请使用 `docker-compose.prod.yml`（将在第七阶段创建）。
