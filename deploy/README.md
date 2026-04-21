# StudySystem 部署说明

## 1. 首次部署

```bash
cd deploy
cp .env.example .env
# 编辑 .env（数据库和邮箱配置）
docker compose up -d --build
```

访问：`http://<服务器IP或域名>`

## 2. 配置文件说明

- `docker-compose.yml`：编排 frontend / backend / mysql 三个服务
- `nginx.conf`：前端静态资源 + `/api/` 反代到后端
- `.env.example`：环境变量模板（复制为 `.env` 后使用）
- `backend.Dockerfile`：构建并运行 Spring Boot
- `frontend.Dockerfile`：构建 Vue 并由 Nginx 托管

## 3. 迁移到 117.72.56.226

在旧服务器执行：

```bash
cd deploy
chmod +x migrate-to-117.72.56.226.sh
./migrate-to-117.72.56.226.sh
```

脚本会：
1. 备份旧服务器 MySQL 数据
2. 打包部署文件并上传到新服务器
3. 在新服务器启动容器并导入数据库

完成后，把域名 A 记录切到 `117.72.56.226`。
