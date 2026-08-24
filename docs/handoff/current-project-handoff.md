# 助农商城当前项目统一交接文档

更新时间：2026-08-24
当前分支：`feature/phase-3-cart-orders`
当前提交：`4cfdbf1 fix: style role workspace dashboards`

## 1. 项目概况

这是一个前后端分离的农产品电商系统，面向三类角色：买家 `USER`、农户 `FARMER`、管理员 `ADMIN`。三类角色使用不同工作区：买家商城 `/`、农户工作台 `/farmer`、管理员后台 `/admin`。`.vscode/` 已忽略，今后不得修改或提交。

## 2. 技术栈与目录

- 后端：Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis Plus、Flyway。
- 前端：Vue 3、TypeScript、Composition API、Vue Router 4、Pinia、Axios、Vite、Vitest。
- 数据与基础设施：MySQL 8、Redis、Redis Lua、Docker Compose。
- 目录：`backend/` 后端，`frontend/` 前端，`docs/` 文档，`docker-compose.yml` 依赖服务。

## 3. 已完成能力

### 认证与权限

- 买家注册登录、农户资质申请、管理员内部登录通道。
- JWT 会话恢复，前后端 RBAC，路由守卫和后端资源所有权校验。
- 农户状态：待审核、已通过、已拒绝；待审核和被拒绝不能登录。
- Flyway V7：`farmer_profiles`、`farmer_audits`；农户拒绝后可复用账号重新提交。

### 商品与审核

- 商品目录、分类、详情、库存、价格、上下架。
- Flyway V6：商品审核字段和 `PENDING_REVIEW`、`REJECTED` 状态。
- 农户创建、修改、重新提交均进入待审核；公开接口只展示 `ON_SALE`。
- 管理员商品审核 API 与页面：筛选、通过、带原因拒绝。
- 农户商品管理页：本人商品、状态、拒绝原因、重新提交和下架。
- 商品图片上传已完成：农户可桌面拖拽/点击选择，移动端从相册选择；后端接口为 `POST /api/farmer/product-images`，仅 `FARMER` 可用，支持 JPG/PNG/WEBP，单文件 5 MB，返回 `/uploads/products/<uuid>.<ext>`。

### 交易与用户中心

- 买家购物车、结算、库存扣减、普通订单、订单详情、再次购买。
- 地址管理、默认地址、收藏、用户资料、手机号和密码修改。
- 农户订单查看和发货，管理员平台订单查看。
- Redis Lua 秒杀：库存预热、原子扣减、一人一单、失败补偿、取消和超时恢复。

### 三端工作区

- 农户工作台：经营概览、商品管理、发布/编辑商品、订单处理和发货。
- 管理员后台：平台概览、商品审核、农户审核、平台订单。
- 独立侧栏、顶栏、移动端菜单、退出登录和角色专属入口。
- 仪表盘使用现有真实 API 聚合，部分接口失败显示“暂不可用”，不伪造统计。

## 4. 已验证的核心业务链路

已完成真实浏览器端到端验收：

1. 农户提交入驻申请，状态为 `PENDING`，申请期间无法登录。
2. 管理员在 `/admin/farmers` 审核通过。
3. 农户登录 `/farmer`，提交商品，商品进入 `PENDING_REVIEW`。
4. 管理员在 `/admin/products` 审核通过，商品变为 `ON_SALE`。
5. 买家商城首页可见商品并成功提交普通订单。

## 5. 未完善功能与优先级

### 必须先做

- 生产数据隔离：清理验收账号和演示商品，区分开发、测试、生产数据。

### 后续交易能力

- 真实支付：支付单号、支付状态、沙箱/正式渠道、回调验签、幂等和超时关闭。
- 物流：物流单号、状态同步和买家展示。
- 评价：评分、文字、图片、重复评价限制和农户回复。
- 售后：退款、售后申请、审核和状态流转。

### 管理与安全增强

- 管理员统计：用户、农户、商品、订单、销售额和排行等聚合 API。
- 管理员账号管理：仅管理员后台创建，超级管理员、运营、客服细分权限暂不实现。
- JWT 撤销：当前 JWT 无状态，改密码后旧令牌最长仍可用约 30 分钟；生产前增加 token version 或服务端撤销列表。

## 6. 图片上传建议

商品图片第一阶段已采用拖拽 + 点击选择 + 移动端相册 + 本地预览，后端保存到 `backend/uploads/products`，限制 JPG/PNG/WEBP、单文件 5 MB，同时校验文件头、MIME 和扩展名，生成 UUID 文件名并返回图片 URL。配置项为 `AGROMALL_UPLOAD_PRODUCT_DIR`。容器化部署需将上传目录挂载为 volume；稳定后可迁移 MinIO/OSS，数据库继续只保存 URL，不保存图片二进制。移动端拍照、多图详情和历史图片复用延期到体验优化阶段。

## 7. 启动与验收

### 环境隔离

后端默认使用 `dev` profile。开发、测试和生产分别使用独立的 MySQL 数据库、Redis key 前缀和商品图片目录：

- `dev`：数据库默认 `agromall`，Redis 前缀 `agromall:dev:`，图片目录 `uploads/products`。
- `test`：数据库默认 `agromall_test`，Redis 前缀 `agromall:test:`，图片目录 `uploads/test-products`。
- `prod`：必须显式设置 `AGROMALL_DB_URL`、`AGROMALL_DB_USERNAME`、`AGROMALL_DB_PASSWORD`、`AGROMALL_REDIS_HOST`、`AGROMALL_REDIS_PORT`、`AGROMALL_REDIS_PASSWORD`、`AGROMALL_REDIS_KEY_PREFIX`、`AGROMALL_JWT_SECRET` 和 `AGROMALL_UPLOAD_PRODUCT_DIR`，缺少值时不使用开发默认值。

PowerShell 选择 profile：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev" # 或 test / prod
cd backend
mvn spring-boot:run
```

测试数据库、Redis 数据和测试上传目录均为可清理数据；验收账号和演示商品不得通过初始化脚本自动进入生产。

商品图片上传目录由 `AGROMALL_UPLOAD_PRODUCT_DIR` 配置，默认是 `backend/uploads/products`。本地运行前创建该目录；容器化部署时将宿主机持久化目录挂载到同一路径，避免容器重建丢失图片。当前 Docker Compose 只启动 MySQL 和 Redis，Spring Boot 进程仍按文档单独启动。

在 PowerShell 中执行：

```powershell
cd C:\Users\chen\Documents\Codex\2026-07-19\xain\agricultural-mall
docker compose up -d

cd backend
mvn spring-boot:run

cd ..\frontend
npm run dev -- --host 0.0.0.0
```

访问 `http://localhost:5173/`。依赖端口：MySQL `3306`、Redis `6379`、后端 `8080`、前端 `5173`。若 `8080` 已有旧 Spring Boot 进程，先停止旧进程再启动，否则浏览器可能访问旧代码。

验收账号：管理员 `acceptadmin / AdminPass819!`；农户 `18800008220 / FarmerPass820!`；买家 `browserbuyer820 / BuyerPass820!`。另有待审核农户 `18800000046 / FarmerTest0046!`，需管理员先审核。

## 8. 验证结果

- 后端全量测试：89 项通过，0 failures，0 errors。
- 前端全量测试：49 项通过。
- 前端生产构建：通过。
- 浏览器验收：管理员、农户、买家桌面和移动端通过；无水平溢出，控制台无错误。

命令：`cd backend; mvn test`；`cd frontend; npm run test -- --run`；`npm run build`；提交前执行 `git diff --check` 和 `git status --short`。

## 9. Git 协作约定

日常开发提交到 `feature/phase-3-cart-orders`，阶段完成后通过 PR 合并到 `main`，不要重写历史或强制推送。禁止提交 `.vscode/`、`.env`、`dist`、`target`、`node_modules` 和本地数据库数据。

## 10. 推荐下一步

图片上传闭环已完成。下一步先做生产数据隔离，再推进沙箱支付；之后按路线文档完善秒杀库存一致性、物流、评价、售后和管理员安全能力。移动端拍照属于最后的体验优化。每项都应补充接口权限测试、前端测试和浏览器回归验收。
