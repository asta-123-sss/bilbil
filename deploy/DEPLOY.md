# 上线部署指南（阿里云轻量服务器，与 fitness-training-agent 完全同套路）

`fitness-training-agent` 现在的访问方式是直接 `http://47.243.8.223:8080/`（IP + 端口，没有域名、没有 Nginx）。
本项目照抄这个套路：**前后端打包成一个 jar，由 Spring Boot 一个端口全托管**，直接 `http://服务器IP:8081/` 访问。

- fitness 用 **8080** 端口 → 本项目用 **8081** 端口，互不冲突。
- 前端 Vue 已经 `npm run build` 后把 `dist/` 塞进 `src/main/resources/static/`，并加了 `SpaController` 兜底 history 路由，所以后端 jar 自己就能返回页面 + 接口 + 视频文件，**不需要 Nginx**。

## 目标架构

```
浏览器
  │  打开 http://47.243.8.223:8081/
  ▼
bilbil-app 容器（Spring Boot，映射宿主机 8081）
  ├── /           前端页面（jar 内置 static）
  ├── /api/**     后端接口
  └── /upload/**  视频/封面文件（挂载的命名卷 upload-data）
                     ├── bilbil-mysql 容器（内网，不映射端口）
                     └── bilbil-redis 容器（内网，不映射端口）
```

## 端口规划

| 服务 | fitness 占用 | 本项目 |
| --- | --- | --- |
| 应用（公网访问） | 8080 | **8081** |
| MySQL | 127.0.0.1:3306 | 不映射宿主机端口 |
| Redis | 127.0.0.1:6379 | 不映射宿主机端口 |

---

## 一、打包（在你本地 Windows 做，可选）

代码已经帮你把前端打进 jar 了，**当前 `target/bilbil-1.0.0.jar` 就是完整的成品**。
如果以后改了前端或后端代码，重新走一遍：

```bash
# 1. 改前端后重新构建并塞进后端 static
cd E:\SpringBoot-bilbil\bilbil-web
npm run build
# 把 dist 拷回后端静态目录
cp -r dist/* ../src/main/resources/static/

# 2. 打后端 jar（会自动带上 static 里的前端）
cd E:\SpringBoot-bilbil
# 用你本地的 Maven（IntelliJ 自带或系统 mvn）：
mvn clean package -DskipTests
```

> 其实用 Docker 在服务器上构建时也会重新编译打包（见下），所以这步「本地打包」只是方便你本地先验证，不是上线必需。

---

## 二、上传到服务器 + 启动

在服务器建目录 `/opt/bilbil`，把下面这些上传进去（**不需要**上传 `bilbil-web/` 前端源码和 `target/`）：

```
/opt/bilbil/
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── .env
├── pom.xml
├── src/            # 后端源码（内含 src/main/resources/static 的前端产物）
└── sql/
    └── bilbil.sql  # 首次启动自动建表
```

本地 Windows 执行上传（替换成你的服务器 IP）：

```bash
scp -r Dockerfile .dockerignore docker-compose.yml .env pom.xml src sql root@你的服务器IP:/opt/bilbil/
```

### 改 `.env` 里的两处

```ini
DB_PASSWORD=root123456                          # 自定义 MySQL 密码
ACCESS_DOMAIN=http://你的服务器IP:8081            # 改成你服务器的公网 IP，端口保持 8081，结尾不要斜杠
```

### 启动

```bash
cd /opt/bilbil
docker compose up -d --build
docker compose ps            # 确认 mysql / redis / app 三个都 Up (healthy)
docker compose logs -f app   # 出现 Started BilbilApplication 即成功
```

首次构建会在服务器下载 Maven 依赖，慢一点正常；之后只改代码重建会走缓存。

---

## 三、阿里云安全组放行端口

控制台 → 轻量服务器 → 防火墙/安全组 → 添加规则，放行 **TCP 8081**（fitness 已放行 8080，8081 同样要放）。

> 3306 / 6379 不要放行，只在容器内网用。

---

## 四、验收

浏览器打开 `http://你的服务器IP:8081/`：

- 能看到首页，视频显示首帧封面；
- 刷新 `/video/1`、`/login`、`/history` 等深层路径不会 404（已由 SpaController 兜底）；
- 播放、上传、投币、历史、私信、后台都能正常用；
- 新注册一个账号，用 SQL 把它提为管理员后能看到「后台管理」入口：

```bash
docker exec -it bilbil-mysql mysql -uroot -p bilbil \
  -e "UPDATE user SET role=1 WHERE username='你的账号';"
```

---

## 常见问题

- **启动后 app 连不上数据库**：确认 `.env` 的 `DB_PASSWORD` 一致；改过密码要清旧数据卷重来：`docker compose down -v && docker compose up -d --build`。
- **视频/封面 404 或打不开**：检查 `.env` 的 `ACCESS_DOMAIN` 是否等于 `http://你的服务器IP:8081`，且阿里云安全组已放行 8081。
- **首次没自动建表**：只有 MySQL 数据卷为**空**时才会执行 `sql/bilbil.sql`；之前起过就 `docker compose down -v` 重来，或手动 `docker exec -i bilbil-mysql mysql -uroot -p bilbil < sql/bilbil.sql`。
- **8081 被占用**：改 `docker-compose.yml` 里 `app` 的 `8081:8080` 为其它端口，并同步改 `.env` 的 `ACCESS_DOMAIN`。
- **刷新深层路径 404**：确认 jar 里 `src/main/resources/static/` 有前端产物、且 `SpaController` 已编译进 jar（正常会打进去）。
- **RocketMQ**：默认关闭（`bilbil.rocketmq.enabled=false`），不依赖 broker，开箱即用。

---

## 以后改前端代码的提醒

前端源码在 `bilbil-web/`，但它不会自动进 jar。改了前端后必须：

1. `cd bilbil-web && npm run build`
2. 把 `dist/*` 拷回 `src/main/resources/static/`
3. 重新 `mvn clean package` 或 `docker compose up -d --build`
