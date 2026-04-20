# Spring Boot Docker部署与CI/CD配置指南

## 项目现状

当前项目已包含：
- `Dockerfile`: 多阶段构建配置，使用Maven构建和OpenJDK运行
- `docker-compose.yml`: Docker Compose配置，包含健康检查和 Nacos 服务
- `pom.xml`: 已添加 Spring Boot Actuator、Spring Cloud Alibaba Nacos 依赖
- **Nacos 服务注册与配置中心**已集成

## Spring Cloud Alibaba + Nacos 集成说明

### 新增功能
1. **服务注册与发现**: 应用启动时自动注册到 Nacos
2. **配置中心**: 支持从 Nacos 动态获取配置
3. **Nacos 控制台**: http://localhost:8848/nacos (默认账号/密码: nacos/nacos)

### 环境变量配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `NACOS_SERVER_ADDR` | Nacos 服务器地址 | 127.0.0.1:8848 |
| `NACOS_NAMESPACE` | Nacos 命名空间 | (空) |
| `NACOS_GROUP` | Nacos 配置分组 | DEFAULT_GROUP |

## 1. GitHub Actions CI/CD配置

已创建 `.github/workflows/ci-cd.yml` 文件，实现以下功能：
- 代码提交到master分支时自动触发
- 使用Docker Buildx构建镜像
- 推送到Docker Hub
- 通过SSH自动部署到云服务器

### 需要配置的GitHub Secrets

在GitHub仓库的Settings > Secrets and variables > Actions中添加以下密钥：

| 密钥名称 | 描述 |
|---------|------|
| `DOCKER_HUB_USERNAME` | Docker Hub用户名 |
| `DOCKER_HUB_TOKEN` | Docker Hub访问令牌 |
| `SERVER_HOST` | 云服务器IP地址 |
| `SERVER_USERNAME` | 云服务器用户名 |
| `SERVER_PASSWORD` | 云服务器密码 |
| `SERVER_PORT` | SSH端口（默认22） |

## 2. 云服务器配置

### 步骤1: 安装Docker和Docker Compose

```bash
# 更新包管理
apt update && apt upgrade -y

# 安装Docker
curl -fsSL https://get.docker.com | bash

# 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 步骤2: 创建项目目录

```bash
mkdir -p /opt/springboot-docker-deploy
cd /opt/springboot-docker-deploy
```

### 步骤3: 上传配置文件

将以下文件上传到服务器目录：
- `docker-compose.yml`

### 步骤4: 配置Docker Compose文件

根据实际情况修改 `docker-compose.yml` 中的镜像名称：

```yaml
version: '3.8'

services:
  app:
    image: your-dockerhub-username/springboot-docker-deploy:latest
    container_name: springboot-app
    ports:
      - "8080:8080"
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xms256m -Xmx512m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - springboot-network

networks:
  springboot-network:
    driver: bridge
```

## 3. 配置Spring Boot Actuator

确保 `application.properties` 中启用了Actuator健康检查：

```properties
# Actuator配置
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

## 4. 测试部署流程

### 本地测试

```bash
# 构建镜像
docker-compose build

# 启动服务（包含 Nacos）
docker-compose up -d

# 查看日志
docker-compose logs -f

# 访问应用
curl http://localhost:8080/

# 健康检查
curl http://localhost:8080/actuator/health

# 访问 Nacos 控制台
open http://localhost:8848/nacos  # 账号/密码: nacos/nacos

# 停止服务
docker-compose down
```

### Nacos 配置管理

1. 访问 Nacos 控制台: http://localhost:8848/nacos
2. 登录账号: nacos / nacos
3. 创建配置:
   - Data ID: `springboot-docker-deploy.yaml`
   - Group: `DEFAULT_GROUP`
   - 配置格式: YAML
   - 配置内容示例:
     ```yaml
     server:
       port: 8080
     spring:
       application:
         name: springboot-docker-deploy
     ```

### CI/CD测试

1. 修改代码并推送到GitHub master分支
2. 进入GitHub仓库的Actions页面查看构建状态
3. 构建完成后，检查云服务器上的服务是否更新

## 5. 监控与维护

### 查看容器状态

```bash
docker-compose ps
docker-compose logs -f
```

### 手动更新服务

```bash
# 拉取最新镜像
docker-compose pull

# 重启服务
docker-compose up -d
```

### 查看资源使用情况

```bash
docker stats
```

## 6. 安全建议

1. 使用非root用户运行容器（已在Dockerfile中配置）
2. 定期更新Docker镜像和依赖
3. 配置防火墙规则，限制访问IP
4. 使用HTTPS加密通信（可添加Nginx作为反向代理）
5. 定期备份数据和配置

## 7. 故障排查

### 容器启动失败

```bash
# 查看详细日志
docker-compose logs --tail=100

# 检查端口占用
netstat -tuln | grep 8080
```

### 健康检查失败

```bash
# 进入容器内部检查
 docker-compose exec app bash

# 在容器内检查应用状态
curl http://localhost:8080/actuator/health
```

### 网络问题

```bash
# 检查容器网络
docker network ls
docker inspect springboot-docker-deploy_springboot-network
```

---

完成以上配置后，每次向GitHub master分支提交代码，都会自动触发CI/CD流程，构建Docker镜像并部署到云服务器。