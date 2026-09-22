# 线上投票小程序

微信小程序端「线上投票」系统：后端提供投票活动的创建 / 发布 / 答题 / 统计闭环，小程序端提供登录、列表、详情、答题、统计、我的记录等页面。

> 本仓库只包含代码与生产配置模板；说明类文档一律留在本地（见「说明类文档」一节）。

---

## 一、项目概览

| 项 | 说明 |
|---|---|
| 后端 | Spring Boot 3.3.5 + MyBatis + MySQL 8 + JWT |
| 前端 | uni-app (Vue 3) + Vite，编译目标微信小程序 / H5 |
| Java | 21 |
| Maven | 3.9.16 |
| 仓库 | https://github.com/Hvlone/vote-system-mini |
| 分支 | main（唯一长期分支） |
| 规模 | 37 个 Java 源文件（含 1 个单元测试） |

---

## 二、目录结构

```text
线上投票小程序/
├── .gitignore
├── README.md                                     # 唯一入库的说明文档
├── sql/
│   └── init.sql                                  # 建库 + 6 张表 DDL + 唯一索引
├── backend/                                      # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/vote/system/
│       │   │   ├── VoteSystemApplication.java
│       │   │   ├── common/                       # 4 个：Result / BusinessException / GlobalExceptionHandler / UserContext
│       │   │   ├── config/                       # 1 个：WebConfig（拦截器注册 + 放行白名单）
│       │   │   ├── controller/                   # 5 个：Health / Auth / Vote / Submit / Stats
│       │   │   ├── dto/                          # 7 个：登录、分页、投票、提交、统计等出入参
│       │   │   ├── entity/                       # 5 个：User / Vote / Question / Option / VoteRecord
│       │   │   ├── interceptor/                  # 1 个：JwtInterceptor
│       │   │   ├── mapper/                       # 5 个：注解方式 SQL
│       │   │   ├── service/                      # 4 个：Auth / Vote / Submit / Stats
│       │   │   └── util/                         # 3 个：JwtUtil / IpUtil / WechatUtil
│       │   └── resources/
│       │       ├── application.yml               # 开发默认配置（敏感项为 ${环境变量:开发默认值}）
│       │       ├── application-prod.yml          # 生产配置（仅 ${} 占位，零密钥）
│       │       └── application-prod.example.yml  # 环境变量清单说明
│       └── test/java/com/vote/system/util/
│           └── JwtUtilTest.java
└── miniapp/                                      # uni-app 前端
    ├── package.json / vite.config.js / index.html
    └── src/
        ├── App.vue / main.js / pages.json / manifest.json / uni.scss
        ├── api/                                  # 3 个：auth / vote / stats
        ├── pages/                                # 7 个页面：login / list / detail / answer / stats / mine / create
        └── utils/                                # 4 个：request / datetime / theme / voteState
```

---

## 三、数据库

`sql/init.sql` 建库 `vote_system` 并建 6 张表：

| 表 | 说明 |
|---|---|
| `user` | 用户（登录即注册；`role`: USER / ADMIN） |
| `vote` | 投票活动（`status`: 0 草稿 / 1 发布中 / 2 已结束） |
| `question` | 题目（`question_type`: 1 单选 / 2 多选 / 3 填空） |
| `option` | 选项（仅单选/多选使用；表名为 MySQL 保留字，SQL 内需加反引号） |
| `vote_record` | 投票记录；唯一索引 `uk_user_vote(openid, vote_id)` 是防重复投票的核心 |
| `ip_limit` | IP 限流规格预留表。当前限流由 `vote_record` 按 `(vote_id, ip_address, 60 分钟窗口)` 实时计数实现，本表暂不写入 |

> `init.sql` 末尾附带的测试数据段仅供本地联调；生产部署只执行建表 DDL 部分。

---

## 四、接口一览

统一前缀 `/api/v1`，统一响应体 `{code, message, data}`；除探活与公开浏览类白名单外，均需携带 `Authorization: Bearer <token>`。

| 控制器 | 数量 | 说明 |
|---|---|---|
| `AuthController` | 1 | `POST /auth/login` 微信登录 |
| `VoteController` | 8 | 创建 / 列表 / 详情 / 公开详情 / 编辑 / 发布 / 停止 / 删除 |
| `SubmitController` | 1 | `POST /{id}/submit` 提交投票 |
| `StatsController` | 3 | 统计 / 明细 / 我的记录 |
| `HealthController` | 1 | `GET /health` 探活 |

---

## 五、本地开发

后端（需 JDK 21 + Maven + 本地 MySQL）：

```powershell
mvn -f backend/pom.xml clean package
java -jar backend\target\vote-system-backend-1.0.0.jar
```

前端（需 Node.js）：

```powershell
cd miniapp
npm install
npm run dev:h5            # H5 联调
npm run build:mp-weixin   # 出微信小程序包
```

后端所有敏感配置项都写成 `${环境变量:开发默认值}`，本地零改动即可运行。生产必须注入 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET`，其中 **`JWT_SECRET` 在生产配置中不设默认值，缺失即启动失败**（刻意设计，防误用开发密钥上线）。

---

## 六、部署

阿里云轻量应用服务器（境内节点）+ MySQL 8 + systemd 托管 jar + Nginx 反向代理；域名对外提供服务前需先完成 ICP 备案。

生产环境变量由服务器侧 `/etc/vote-system/vote-system.env`（权限 600、属主 root）注入，其中的真实密钥一律不入库。详细步骤、验证清单与排障表见本地实施文档。

---

## 七、说明类文档

`.gitignore` 含 `*.md`：说明类文档一律不入库，**仅 `README.md` 例外**（由 `!README.md` 显式放行）。本地文档集中在 `实现文档/`：

```text
实现文档/
├── 01-需求与总体方案/    # 功能清单、技术架构方案、减法版架构方案、项目实现步骤、搭建详细流程
└── 02-分步实现/          # 按步骤归档：第 1 步（数据库）~ 第 6 步（后端部署）
                          # 含代码解析、架构解读、测试用例与报告、核心流程时序图等
```

确有需要单独上传某个 `.md` 时，用 `git add -f <文件名>` 强制加入，并在提交前确认。

---

## 八、提交约定

- 仅上传代码文件；说明类 `*.md` 一律不上传，仅 `README.md` 例外
- 一次只提交一个逻辑单元，按文件名 `git add`，禁止 `git add -A` / `-u` 全量暂存
- 提交 / 推送前先展示 commit 描述并等待确认，确认后再执行 commit / push
- 提交信息全中文，标题格式为 `fix:` / `feat:` / `chore:` / `refactor:` / `docs:` / `perf:` / `test:` / `style:` + 一句话，正文与真实 diff 一一对应
- 本地留档（均不入库）：fix → `已解决问题.md`、feat → `新增功能.md`、其余类型 → `变更记录.md`

---

## 九、关键提交记录

| 提交 | 类型 | 要点 |
|---|---|---|
| `89cb8f2` | P0 | 公开详情 `GET /votes/{id}/visible` 迁至 VoteController 并支持可选 token（有效 token 时 `hasVoted` 正确，无/无效 token 匿名放行，草稿一律 404）；SubmitController 仅保留 `POST /{id}/submit`；VoteService 新增 `validateQuestions`（题型必须 ∈ {1,2,3}，单选/多选必带选项，填空不带选项，否则 400） |
| `ce5006d` | P1 | 多选题重复选项返回 400；`/my/records` 与 `/votes/{id}/details` 记录新增 `voteTitle`；详情组装收敛到 `VoteService.buildDetail`；IpUtil 改为 `@Component` 并新增 `ip.trust-proxies`（默认 false 仅取 remoteAddr）；删除无效的 `mybatis.mapper-locations`；`sql/init.sql` 中 `ip_limit` 标注为规格预留表 |
| `bec701a` | P2 | 详情 / 列表新增 `expired` 推导字段（已到期但 `status` 仍为 1 时 `expired=true`，状态机不变） |
| `27da4e5` | 修复 | 未匹配路由与方法不支持被兜底成 500 → 改为 404 / 405 |
| `1448eaf` | 修复 | prod profile 未生效导致线上调试登录被开启：prod 配置转为正式文件入库、`debug-login-enabled` 默认值改 false、移除对应忽略项 |
| `f46306b` | 修复 | 登录页调试入口改为仅本地开发构建可见 |
