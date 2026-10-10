# 仿B站视频平台（全栈：Spring Boot 后端 + Vue3 前端）

一个仿照 B 站（Bilibili）的视频分享平台全栈项目：后端重点实现了 **视频分块上传 / 断点续传 / 秒传**、**个性化推荐**、**广告投放（CPM 计费）** 与 **AI 内容审核** 等核心能力，并围绕高并发读场景落地了 Redis 缓存、布隆过滤器防穿透、计数异步落库等工程实践；前端基于 **Vue3 + Element Plus** 实现了登录注册、视频列表、分块上传（断点续传/秒传/进度条）、视频播放（弹幕/点赞/收藏/评论/投币/关注）、分区频道、商家中心、消息中心等完整页面，前后端一体化打包、Docker 一键部署于阿里云。

> 技术栈：Spring Boot 3 · MyBatis · MySQL 8 · Redis · Vue3 · Element Plus · Docker · DeepSeek · 通义千问 Qwen-VL · ffmpeg

---

## 一、项目介绍

本项目是一个完整的视频平台服务端，覆盖「用户 — 视频 — 互动 — 消息」完整链路：

| 模块 | 能力 |
| --- | --- |
| 用户模块 | 注册 / 登录 / 退出登录（Redis token 会话）、个人信息修改、**修改密码**、头像上传、用户主页、关注 / 粉丝 |
| 视频模块 | **分块上传 + 断点续传 + 秒传 + 分片合并**、封面图上传、视频列表 / 详情 / 播放、投稿管理、**作者编辑 / 删除自己的视频**、**观看历史** |
| 互动模块 | 点赞 / 取消点赞、收藏 / 取消收藏、**投币 / 取消投币**、评论（含回复/子评论）、弹幕（MySQL 存储） |
| 消息模块 | 点赞 / 评论 / 回复 / 关注 异步通知（RocketMQ 异步投递 + 降级直接落库）、**点对点私信** |
| 广告模块 | 商家发布广告（CPM 计费）、兴趣定向投放、曝光扣费 / 预算控制、点击统计 |
| 商家模块 | 商家申请 / 审核、积分充值、商家自助管理广告 |
| 内容审核 | 发布视频 / 广告前 **AI 自动审核**：DeepSeek 文本 + 通义千问 Qwen-VL 视觉 + ffmpeg 视频抽帧，违规自动拦截 |
| 后台模块 | **管理员审核**：视频下架 / 恢复、用户封禁 / 解封、违规评论删除、商家申请审核 / 积分充值、广告管理 |
| 分区模块 | 视频分类列表（Redis 缓存）+ 分区频道页 |

### 核心设计亮点

1. **分块上传**：大文件按分片上传，客户端可并发、可断点续传；分片合并入库前做完整性与连续性校验。
2. **秒传**：整文件 MD5 命中历史视频时直接复用，无需再次上传。
3. **缓存穿透 / 击穿 / 雪崩**：Redis 位图布隆过滤器拦截不存在 ID；计数类数据走 Redis Set / 计数器；缓存 TTL 加随机抖动避免雪崩。
4. **高并发计数**：点赞 / 收藏数由 Redis Set 原子维护（SADD/SREM），播放量 INCR 自增，通过定时任务批量回写 MySQL，避免高频写库。
5. **Redis 原子会话**：登录 token 存入 Redis，支持滑动续期与退出删除。
6. **异步解耦**：点赞 / 评论 / 关注 / 转码等消息通过 RocketMQ 异步处理，未部署 broker 时优雅降级为本地直写，开箱即用。
7. **AI 内容审核**：发布视频 / 广告前，用 DeepSeek 审核标题简介标签，用通义千问 Qwen-VL 审核封面图与 ffmpeg 抽取的视频帧；未配 Key 或调用异常时自动 fail-open 放行，不阻塞正常发布。
8. **广告投放**：兴趣定向投放（按用户偏好分区）、CPM 计费（每千次曝光扣费）、预算耗尽自动下线、余额不足自动暂停。

---

## 二、技术栈

| 技术 | 用途 | 版本 |
| --- | --- | --- |
| Spring Boot | 基础框架 | 3.2.5 |
| Java | 运行环境 | 17 |
| MyBatis | 数据访问（注解式 Mapper） | 3.0.3 |
| MySQL | 关系型数据库 | 8.x |
| Redis | 热点缓存 / 会话 / 计数 / 布隆过滤器 | — |
| RocketMQ | 异步消息（转码、通知） | client 4.9.7 |
| FastDFS | 分布式文件存储（可选，默认本地存储） | — |
| Nginx | 静态资源 / 视频访问代理（生产） | — |
| Docker | 容器化部署（app / mysql / redis 编排） | — |
| DeepSeek | AI 文本审核（deepseek-chat） | — |
| 通义千问 Qwen-VL | AI 视觉审核（封面图 / 视频帧） | qwen-vl-max |
| ffmpeg | 视频抽帧（审核用） | — |
| Lombok / Hutool | 开发提效工具 | 5.8.26 |
| Validation | 参数校验（jakarta） | — |

---

## 三、项目结构

```
bilbil
├── sql/
│   └── bilbil.sql                  # 建库建表 SQL（含分区种子数据）
├── src/main/java/com/bilbil/
│   ├── BilbilApplication.java      # 启动类
│   ├── common/
│   │   ├── constant/               # 常量（Redis Key / 视频状态 / 消息类型）
│   │   ├── exception/              # 业务异常 + 全局异常处理器
│   │   ├── result/                 # 统一返回 Result / ResultCode / PageResult
│   │   └── util/                   # UserContext、布隆过滤器工具
│   ├── config/                     # 配置类（Redis / Web / RocketMQ / 业务属性）
│   ├── controller/                 # 接口层（16 个 Controller）
│   ├── service/                    # 业务层（含 ContentModerationService 审核、FrameExtractor 抽帧）
│   ├── mapper/                     # MyBatis 数据访问层
│   ├── entity/  dto/  vo/          # 实体 / 入参 / 出参
│   ├── interceptor/                # 登录拦截器
│   ├── storage/                    # 文件存储抽象（本地 / FastDFS）
│   ├── mq/                         # RocketMQ 生产者 / 消费者
│   └── task/                       # 定时任务（计数异步落库）
└── src/main/resources/application.yml
```

---

## 四、数据库设计

共 **15 张表**（建表 SQL 见 [sql/bilbil.sql](sql/bilbil.sql)，已有库升级见 [sql/upgrade_v2.sql](sql/upgrade_v2.sql) / [sql/upgrade_v3.sql](sql/upgrade_v3.sql)）：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户（含头像、性别、生日、签名、**角色 role** 等） |
| `video` | 视频（含 file_id / video_url、状态、可见性、各计数、**投币数**） |
| `video_chunk` | 视频分片（upload_id + chunk_index 唯一，支持断点续传） |
| `video_category` | 视频分区（游戏 / 生活 / 科技 / 动画 / 音乐 / 知识） |
| `user_follow` | 关注关系 |
| `video_like` | 点赞关系 |
| `video_collect` | 收藏关系 |
| `video_coin` | 投币关系 |
| `watch_history` | 观看历史 |
| `comment` | 评论（parent_id / root_id 支持回复与子评论） |
| `danmaku` | 弹幕（按视频时间点存储） |
| `message_notify` | 消息通知 |
| `private_message` | 私信（点对点） |
| `merchant_apply` | 商家申请（提交 / 审核 / 拒绝） |
| `ad` | 广告（CPM / 预算 / 曝光 / 点击 / 消耗 / 投放状态） |

> 注意：视频的点赞数 / 收藏数 / 投币数 / 播放量**不实时更新主表**，以 Redis 为准，由定时任务 `CountFlushTask` 每 30 秒批量回写 `video` 表。

---

## 五、部署步骤

### 1. 环境准备

- JDK 17+
- MySQL 8.x、Redis 5+
- Maven 3.6+
- （可选）RocketMQ、FastDFS —— 本项目默认**不依赖**，关闭开关即可本地运行

### 2. 初始化数据库

```sql
-- 手动执行建表脚本
mysql -uroot -p < sql/bilbil.sql
```

脚本会自动创建数据库 `bilbil` 及 10 张表，并插入 6 条分区种子数据。

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bilbil?...   # 按需修改
    username: root
    password: 123456                              # 修改为你的数据库密码
  data:
    redis:
      host: localhost
      port: 6379

bilbil:
  storage:
    type: local                # local 本地存储（默认）| fastdfs 分布式存储
    local-path: ./upload       # 本地文件保存目录
    access-domain: http://localhost:8080
  rocketmq:
    enabled: false             # 开启后需部署 RocketMQ NameServer
  moderation:
    enabled: true
    text-api-key: ${DEEPSEEK_API_KEY:}      # DeepSeek 文本审核
    vision-api-key: ${DASHSCOPE_API_KEY:}   # 通义千问 Qwen-VL 视觉审核
```

> AI 内容审核默认开启，但**未配置对应 Key 时对应环节自动跳过**（fail-open），不影响本地运行。

### 4. 启动

```bash
mvn clean package -DskipTests
java -jar target/bilbil-*.jar
# 或直接 mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080`。

### 5. 启用 RocketMQ（可选）

1. 部署 RocketMQ NameServer / Broker（默认 `127.0.0.1:9876`）；
2. 配置 `bilbil.rocketmq.enabled: true` 并调整 `name-server`；
3. 重启应用，转码与通知消息将走异步消费。

### 6. 启用 FastDFS（可选）

1. 部署 FastDFS tracker / storage；
2. 配置 `bilbil.storage.type: fastdfs` 与 `bilbil.fastdfs.tracker-servers`；
3. 重启应用，视频与封面将上传至 FastDFS。

### 7. Docker 一键部署（生产 / 阿里云）

完整生产部署见 [deploy/DEPLOY.md](deploy/DEPLOY.md)，核心流程：

1. 本地 `mvn clean package -DskipTests` 打出含前端的 jar；
2. 把 jar、`Dockerfile`、`docker-compose.yml`、`sql/`、`.env` 上传到服务器 `/opt/bilbil`；
3. 在 `.env` 里配置 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、`DASHSCOPE_API_KEY`、`ACCESS_DOMAIN` 等；
4. `docker compose up -d --build`，浏览器直接访问 `http://服务器IP:8081/`。

> 前端已打包进 jar 由 Spring Boot 单端口托管，无需 Nginx。

---

## 六、接口说明

统一返回结构：`{ "code": 200, "msg": "操作成功", "data": ... }`，错误码见 `ResultCode`。

> 鉴权方式：登录接口返回 `token`，后续需登录的接口在请求头携带 `token`。

### 用户模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/user/register` | 注册 | 否 |
| POST | `/api/user/login` | 登录，返回 token | 否 |
| POST | `/api/user/logout` | 退出登录 | 是（header token） |
| GET | `/api/user/me` | 当前登录用户信息 | 是 |
| PUT | `/api/user/info` | 修改个人信息 | 是 |
| PUT | `/api/user/password` | 修改密码 | 是 |
| POST | `/api/user/avatar` | 上传头像（multipart: file） | 是 |
| GET | `/api/user/profile/{userId}` | 用户主页（关注/粉丝数） | 否 |

### 视频模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/video/upload/init` | 初始化分块上传（断点续传 / 秒传） | 是 |
| POST | `/api/video/upload/chunk` | 上传分片（multipart: uploadId/chunkIndex/chunkMd5/file） | 是 |
| POST | `/api/video/upload/merge` | 合并分片并提交视频 | 是 |
| POST | `/api/video/cover` | 上传封面（multipart: file） | 是 |
| GET | `/api/video/list` | 视频列表（categoryId/keyword/orderBy/page/size） | 否 |
| GET | `/api/video/detail/{id}` | 视频详情 | 否 |
| GET | `/api/video/play/{id}` | 播放（播放量 +1） | 否 |
| GET | `/api/video/my` | 我的投稿（page/size） | 是 |
| PUT | `/api/video/{id}` | 修改视频信息 | 是 |
| DELETE | `/api/video/{id}` | 下架 / 删除视频 | 是 |

### 互动模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/video/{videoId}/like` | 点赞 | 是 |
| DELETE | `/api/video/{videoId}/like` | 取消点赞 | 是 |
| POST | `/api/video/{videoId}/collect` | 收藏 | 是 |
| DELETE | `/api/video/{videoId}/collect` | 取消收藏 | 是 |
| POST | `/api/video/{videoId}/coin` | 投币 | 是 |
| DELETE | `/api/video/{videoId}/coin` | 取消投币 | 是 |
| POST | `/api/comment/publish` | 发布评论 / 回复 | 是 |
| DELETE | `/api/comment/{id}` | 删除评论 | 是 |
| GET | `/api/comment/list` | 一级评论列表（videoId/page/size） | 否 |
| GET | `/api/comment/replies` | 回复列表（videoId/rootId/page/size） | 否 |
| POST | `/api/danmaku/send` | 发送弹幕 | 是 |
| GET | `/api/danmaku/list` | 弹幕列表（videoId/from/to） | 否 |

### 广告模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/ad` | 发布广告 | 是（商家） |
| GET | `/api/ad/my` | 我的广告列表 | 是（商家） |
| PUT | `/api/ad/{id}/status` | 暂停 / 继续 / 结束广告 | 是（商家） |
| DELETE | `/api/ad/{id}` | 删除广告 | 是（商家） |
| POST | `/api/ad/cover` | 上传广告图 | 是（商家） |
| GET | `/api/ad/feed` | 信息流广告（兴趣定向投放） | 否 |
| POST | `/api/ad/{id}/click` | 广告点击（CTR 统计） | 否 |

### 商家模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/merchant/apply` | 提交商家申请 | 是 |
| GET | `/api/merchant/apply` | 查询我的最新申请 | 是 |

### 关注 / 分区 / 消息模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/follow/{userId}` | 关注 | 是 |
| DELETE | `/api/follow/{userId}` | 取消关注 | 是 |
| GET | `/api/follow/{userId}/followings` | 关注列表 | 是 |
| GET | `/api/follow/{userId}/fans` | 粉丝列表 | 是 |
| GET | `/api/category/list` | 分区列表 | 否 |
| GET | `/api/message/list` | 消息列表 | 是 |
| GET | `/api/message/unread-count` | 未读消息数 | 是 |
| PUT | `/api/message/read/{id}` | 标记单条已读 | 是 |
| PUT | `/api/message/read-all` | 全部已读 | 是 |

### 观看历史 / 私信 / 后台管理

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/history` | 观看历史列表（page/size） | 是 |
| POST | `/api/video/{videoId}/watch` | 上报观看进度 | 是 |
| DELETE | `/api/history/{videoId}` | 删除单条历史 | 是 |
| DELETE | `/api/history` | 清空历史 | 是 |
| POST | `/api/pm/send` | 发送私信 | 是 |
| GET | `/api/pm/conversations` | 会话列表 | 是 |
| GET | `/api/pm/chat/{otherUserId}` | 聊天记录 | 是 |
| GET | `/api/pm/unread-count` | 未读私信数 | 是 |
| PUT | `/api/pm/read/{otherUserId}` | 标记会话已读 | 是 |
| GET | `/api/admin/videos` | 视频列表（审核） | 是（管理员） |
| PUT | `/api/admin/video/{id}/status` | 视频上架/下架 | 是（管理员） |
| GET | `/api/admin/users` | 用户列表 | 是（管理员） |
| PUT | `/api/admin/user/{id}/status` | 用户封禁/解封 | 是（管理员） |
| GET | `/api/admin/comments` | 评论列表 | 是（管理员） |
| DELETE | `/api/admin/comment/{id}` | 删除违规评论 | 是（管理员） |
| GET | `/api/admin/merchant-applies` | 商家申请列表 | 是（管理员） |
| PUT | `/api/admin/merchant-apply/{id}/review` | 审核商家申请 | 是（管理员） |
| GET | `/api/admin/merchants` | 商家列表（含积分余额） | 是（管理员） |
| PUT | `/api/admin/merchant/{id}/recharge` | 商家积分充值 | 是（管理员） |
| GET | `/api/admin/ads` | 广告列表 | 是（管理员） |
| PUT | `/api/admin/ad/{id}/status` | 广告上架 / 暂停 / 结束 | 是（管理员） |

---

## 七、设计说明

- **统一返回**：所有接口返回 `Result<T>`，异常由 `GlobalExceptionHandler` 统一兜底，不向外暴露堆栈。
- **参数校验**：入参 DTO 使用 `@Valid` + 校验注解（jakarta.validation），校验失败统一返回 `400` 与友好提示。
- **登录态**：`LoginInterceptor` 基于白名单放行公开接口，其余接口强制校验 Redis token，支持滑动续期。
- **文件存储可插拔**：`FileStorageService` 抽象本地存储与 FastDFS，切换配置即可。
- **计数一致性**：互动计数以 Redis 为准、MySQL 为兜底，Set 缺失时从关系表回填，兼顾一致性与性能。

---

## 八、Web 前端（bilbil-web）

前端位于仓库根目录的 `bilbil-web/` 子目录，与后端同仓库，构成「一个仓库、前后端两部分」的完整工程。

### 技术栈

Vue 3 + Vite + Vue Router + Pinia + Element Plus + @element-plus/icons-vue + axios + spark-md5

### 目录结构

```
bilbil-web/
├── index.html
├── package.json
├── vite.config.js            # /api 代理到 http://localhost:8080
└── src/
    ├── main.js               # createApp + ElementPlus + 全量图标注册
    ├── App.vue               # 顶部导航（登录态 / 消息未读数）+ <router-view>
    ├── router/index.js       # 路由 + 登录守卫
    ├── stores/auth.js        # Pinia：token/user 持久化
    ├── api/                  # request.js（axios 封装） + 各模块接口
    ├── utils/chunkUpload.js  # 分块上传核心（秒传 / 断点续传 / 并发缺块）
    ├── components/           # VideoCard / DanmakuLayer / CommentSection / BannerCarousel（轮播：广告+热门视频）/ AdCard
    └── views/                # Login / Register / Home / Category / Upload / Play / Profile / Message / History / PrivateMessage / Merchant / Admin
```

### 启动步骤

```bash
cd bilbil-web
npm install
npm run dev            # 默认 http://localhost:5173
```

开发环境已配置 Vite 代理：`/api` → `http://localhost:8080`，无需处理跨域；视频 / 封面为后端返回的绝对 URL，`<video>` / `<img>` 直接使用。

### 分块上传实现要点（前端侧）

- 用 `spark-md5` **增量**计算整文件 MD5（分片读取，不卡主线程）；
- `POST /video/upload/init` 返回 `uploadId` + `uploadedIndexes`（断点续传）+ `alreadyExists`（秒传）；
- 已上传分片跳过，缺失分片按 5MB 切片、并发 3 上传，进度回调驱动进度条；
- 秒传命中时直接跳转已有视频；否则填元数据后 `POST /video/upload/merge` 合并入库并跳转播放页。

### 端到端演示流程

1. 启动 MySQL / Redis，启动后端（8080）；
2. `cd bilbil-web && npm run dev`；
3. 打开 `http://localhost:5173`：注册 → 登录 → 投稿（观察分块进度/秒传）→ 播放（弹幕滚动/发送）→ 点赞/收藏/评论/回复 → 作者主页关注 → 消息中心未读/已读。

---

## 九、第二阶段功能（投币 / 观看历史 / 私信 / 修改密码 / 后台审核）

在第一阶段基础上补齐了 6 项功能，后端沿用既有「Redis Set + 关系表 upsert + dirty 集合 + 定时回写」与「拦截器白名单鉴权」套路，前端沿用既有组件/交互写法。

### 功能清单

| 功能 | 后端要点 | 前端入口 |
| --- | --- | --- |
| **投币** | 新增 `video_coin` 关系表 + `CoinService`（镜像点赞/收藏的 Redis Set 计数套路），`video.coin_count` 异步回写 | 播放页「投币」按钮（数量 +1/-1、已投币态） |
| **观看历史** | 新增 `watch_history` 表，`ON DUPLICATE KEY` 去重刷新进度；`play()` 自动记录，`/watch` 上报进度 | 顶部「历史」页（删除 / 清空）；播放页暂停/离开时上报进度 |
| **修改密码** | `ChangePasswordDTO` + `UserService.changePassword`（BCrypt 校验旧密码） | 个人中心「修改密码」弹窗 |
| **作者编辑/删除视频** | 复用既有 `PUT/DELETE /video/{id}`（已做归属校验） | 我的投稿卡片「编辑 / 删除」按钮 + 编辑弹窗 |
| **私信** | 新增 `private_message` 表 + `PrivateMessageService`（会话分组 / 未读计数 / 自动已读） | 顶部「私信」页（会话列表 + 聊天窗）+ 未读徽标；他人主页「发私信」 |
| **后台审核** | `user.role`（0 普通 / 1 管理员）+ `AdminService/AdminController`，`requireAdmin()` 鉴权 | 管理员账号下拉菜单「后台管理」：视频下架/恢复、用户封禁/解封、删除违规评论 |

### 迁移步骤（已建库环境）

```bash
# 1. 增量升级数据库（新增 role / coin_count / video_coin / watch_history / private_message）
mysql -uroot -p123456 bilbil < sql/upgrade_v2.sql

# 2. 把某个账号提升为管理员（示例：username='admin'）
#    upgrade_v2.sql 末尾已包含：UPDATE user SET role = 1 WHERE username = 'admin';
#    也可手动执行：UPDATE user SET role = 1 WHERE username = '你的账号';
```

### 产品约定

- **新投稿默认直接发布**：`merge` 仍置为已发布状态；后台「审核」为事后下架 / 恢复（`status` 2↔1）。
- **投币为纯计数开关**：仅 `video.coin_count` +1/-1，无用户余额体系。
- **管理员无注册入口**：直接通过 SQL 将 `user.role` 置 1；被封禁用户登录时返回「账号已被封禁」。
