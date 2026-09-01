# GitHub 上传说明 — 第二步 + 第三步

> 本文档说明从零到当前阶段所做的全部新增和改动，用于首次推送到 GitHub 时编写 commit message 和 README。

---

## 一、项目概览

| 项 | 说明 |
|---|---|
| 项目名 | 线上投票小程序（减法版） |
| 当前阶段 | 后端骨架 + 业务接口完成（第 2-3 步） |
| 技术栈 | Spring Boot 3.3.5 + MyBatis + MySQL 8 + JWT |
| Java 版本 | 21 |
| Maven | 3.9.16 |

---

## 二、第二步：后端骨架（首次提交）

### Commit 建议消息

```
feat: 搭建后端骨架 — 统一响应 + JWT认证 + 异常处理

- Spring Boot 3.3.5 工程初始化（pom.xml + application.yml）
- 统一响应层：Result<T> 标准格式 + BusinessException + GlobalExceptionHandler
- JWT认证层：JwtUtil(生成/解析/防篡改) + JwtInterceptor(拦截器) + UserContext(线程存储)
- 工具类：IpUtil(获取客户端IP)
- WebConfig：拦截器注册 + 放行白名单(/health, /auth/login)
- HealthController：验证管道通畅
```

### 新增文件清单（10 个源文件 + 2 配置）

```
backend/
├── pom.xml                                    # 依赖：Spring Boot / MyBatis / MySQL / JWT / Lombok
├── src/main/resources/
│   └── application.yml                        # 数据源 + JWT密钥 + 端口8080
└── src/main/java/com/vote/system/
    ├── VoteSystemApplication.java            # 启动类
    ├── common/
    │   ├── Result.java                       # 统一响应 {code, message, data}
    │   ├── BusinessException.java            # 业务异常（400/401/403/404/409/500）
    │   ├── GlobalExceptionHandler.java       # 全局异常兜底 → Result
    │   └── UserContext.java                  # ThreadLocal 存取当前用户
    ├── util/
    │   ├── JwtUtil.java                      # JWT 生成/解析/校验
    │   └── IpUtil.java                       # 客户端 IP 获取
    ├── interceptor/
    │   └── JwtInterceptor.java               # Token 拦截 + UserContext 注入
    ├── config/
    │   └── WebConfig.java                    # 拦截器注册 + 白名单
    └── controller/
        └── HealthController.java             # GET /api/v1/health
```

---

## 三、第三步：后端业务接口（第二次提交）

### Commit 建议消息

```
feat: 实现全部12个业务接口 — 投票闭环完成

- Entity层：User/Vote/Question/Option/VoteRecord（5个实体）
- DTO/VO层：LoginDTO/LoginVO/VoteCreateDTO/VoteVO/SubmitDTO/StatsVO（6个）
- Mapper层：5个Mapper（注解方式SQL，option表加反引号）
- Service层：AuthService(微信登录)/VoteService(事务CRUD)/SubmitService(防重复)/StatsService(聚合统计)
- Controller层：AuthController/VoteController/SubmitController/StatsController
- 启动类追加 @MapperScan
- WebConfig 追加放行 /votes/*/visible（公开浏览）
```

### 新增文件清单（24 个源文件）

```
backend/src/main/java/com/vote/system/
├── entity/                          # 5个 — 对应数据库表
│   ├── User.java
│   ├── Vote.java
│   ├── Question.java
│   ├── Option.java
│   └── VoteRecord.java
├── dto/                             # 6个 — 接口入参/出参
│   ├── LoginDTO.java                # 登录入参
│   ├── LoginVO.java                 # 登录返回(token+用户信息)
│   ├── VoteCreateDTO.java           # 创建投票(含嵌套QuestionDTO/OptionDTO)
│   ├── VoteVO.java                  # 投票详情(含嵌套QuestionVO)
│   ├── SubmitDTO.java               # 提交投票(含嵌套Answer)
│   └── StatsVO.java                 # 统计(含嵌套QuestionStats/OptionStats)
├── mapper/                          # 5个 — 数据访问(注解SQL)
│   ├── UserMapper.java
│   ├── VoteMapper.java
│   ├── QuestionMapper.java
│   ├── OptionMapper.java            # option表名加反引号(MySQL保留字)
│   └── VoteRecordMapper.java
├── service/                         # 4个 — 业务逻辑
│   ├── AuthService.java             # 登录:微信code2session→查/建用户→签JWT
│   ├── VoteService.java             # 投票:事务建/改/删/发布/停止
│   ├── SubmitService.java           # 提交:防重复+IP限流+必填校验
│   └── StatsService.java           # 统计:总数+选项占比+明细+记录
└── controller/                      # 4个 — 接口入口
    ├── AuthController.java          # POST /api/v1/auth/login
    ├── VoteController.java          # 7个:创建/列表/详情/编辑/发布/停止/删除
    ├── SubmitController.java        # 2个:公开浏览/提交投票
    └── StatsController.java         # 3个:统计/明细/我的记录
```

### 修改的现有文件（2 个）

| 文件 | 改动内容 |
|---|---|
| `VoteSystemApplication.java` | 追加 `@MapperScan("com.vote.system.mapper")` |
| `config/WebConfig.java` | 放行白名单追加 `/api/v1/votes/*/visible` |
| `common/GlobalExceptionHandler.java` | `handleException` 暂时输出异常详情（开发模式辅助调试） |

---

## 四、数据库（首次提交）

### Commit 建议消息

```
feat: 数据库初始化 — 6张表 + 防重复唯一索引 + 测试数据
```

### 文件

```
sql/
└── init.sql    # 建库 + 6张表DDL + 唯一索引uk_user_vote + 测试数据
```

---

## 五、文档（可选单独提交或合并）

### Commit 建议消息

```
docs: 添加架构方案与实现步骤文档
```

### 文件清单

```
├── 减法版架构方案.md              # 功能减法 + 技术选型 + 数据模型 + 接口列表
├── 项目实现步骤.md                # 0-6步实现顺序与里程碑
├── 搭建详细流程.md                # 每步具体命令/文件/验证方法
├── 第2步代码解析.md               # 骨架四层结构与调用链解读
├── 第2步测试文档.md               # 7项测试用例(T1-T7)
└── 第3步代码架构与解读.md          # 业务四层结构与接口调用链解读
```

---

## 六、推荐的 GitHub 操作步骤

### 1. 在 GitHub 上新建仓库

- 仓库名：`vote-system-mini`（或自定义）
- 可见性：Private（推荐）
- 不要勾选 "Add a README"（本地已有文件）

### 2. 本地初始化 + 提交

```powershell
cd "e:\大学\线上投票小程序"

# 初始化 git
git init

# 创建 .gitignore
# （内容见下方）

# 首次提交：全部内容
git add .
git commit -m "feat: 项目初始化 — 数据库 + 后端骨架 + 业务接口 + 文档"

# 关联远程仓库（替换为你的地址）
git remote add origin https://github.com/你的用户名/vote-system-mini.git

# 推送
git branch -M main
git push -u origin main
```

### 3. 如果想分两次提交（更清晰的历史记录）

```powershell
# 第一次：数据库 + 骨架 + 文档
git add sql/ backend/pom.xml backend/src/main/resources/ backend/src/main/java/com/vote/system/VoteSystemApplication.java backend/src/main/java/com/vote/system/common/ backend/src/main/java/com/vote/system/util/ backend/src/main/java/com/vote/system/interceptor/ backend/src/main/java/com/vote/system/config/ backend/src/main/java/com/vote/system/controller/HealthController.java
git commit -m "feat: 数据库 + 后端骨架(统一响应/JWT/异常处理)"

# 第二次：业务接口
git add backend/src/main/java/com/vote/system/entity/ backend/src/main/java/com/vote/system/dto/ backend/src/main/java/com/vote/system/mapper/ backend/src/main/java/com/vote/system/service/ backend/src/main/java/com/vote/system/controller/AuthController.java backend/src/main/java/com/vote/system/controller/VoteController.java backend/src/main/java/com/vote/system/controller/SubmitController.java backend/src/main/java/com/vote/system/controller/StatsController.java
git commit -m "feat: 实现12个业务接口(Entity/DTO/Mapper/Service/Controller)"

git push -u origin main
```

---

## 七、.gitignore 内容

在项目根目录创建 `.gitignore`：

```gitignore
# 编译产物
backend/target/
*.jar
*.class

# IDE
.idea/
*.iml
.vscode/
*.swp

# 系统
Thumbs.db
Desktop.ini
.DS_Store

# Maven
.mvn/
mvnw
mvnw.cmd

# 日志
*.log

# 环境配置（含密码，不要上传）
backend/src/main/resources/application-prod.yml

# 临时测试文件
*.json.bak
```

---

## 八、完整目录结构（当前阶段）

```
线上投票小程序/
├── .gitignore
├── sql/
│   └── init.sql                          # 数据库初始化脚本
├── backend/
│   ├── pom.xml                           # Maven 依赖
│   ├── src/
│   │   ├── main/
│   │   │   ├── resources/
│   │   │   │   └── application.yml      # 配置文件
│   │   │   └── java/com/vote/system/
│   │   │       ├── VoteSystemApplication.java
│   │   │       ├── common/              # 4个公共类
│   │   │       ├── entity/              # 5个实体
│   │   │       ├── dto/                 # 6个DTO/VO
│   │   │       ├── mapper/              # 5个Mapper
│   │   │       ├── service/             # 4个Service
│   │   │       ├── controller/          # 5个Controller
│   │   │       ├── util/                # 2个工具类
│   │   │       ├── interceptor/          # 1个拦截器
│   │   │       └── config/              # 1个配置类
│   │   └── test/
│   │       └── java/com/vote/system/util/
│   │           └── JwtUtilTest.java     # JWT单元测试
│   └── target/                           # (gitignore，不上传)
├── 减法版架构方案.md
├── 项目实现步骤.md
├── 搭建详细流程.md
├── 第2步代码解析.md
├── 第2步测试文档.md
├── 第3步代码架构与解读.md
├── GitHub上传说明.md                     # ← 本文档
└── 功能清单文档.docx                      # 原始需求
```

**统计：** 34 个 Java 源文件 + 7 个文档 + 1 个 SQL + 2 个配置 = 44 个文件
