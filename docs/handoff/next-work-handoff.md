# 助农商城后续开发交接文档

更新时间：2026-08-19
当前分支：`feature/phase-3-cart-orders`

## 当前状态

项目已经具备真实后端，不是只有前端页面。已完成注册登录、JWT、USER/FARMER/ADMIN 权限、商品目录、农户商品创建/修改/上下架、商品审核状态基础、首页展示、购物车、普通订单、Redis Lua 秒杀、用户中心、收货地址、收藏列表和用户资料接口。

P0-1 已完成：Flyway 已迁移到 V6，商品新增 `PENDING_REVIEW`、`REJECTED` 状态及审核字段；农户创建、修改和重新上架都会进入待审核，公开接口仍只返回 `ON_SALE`。P0-2 后端审核 API 已完成。P0-3/P0-4 前端农户商品状态页和管理员审核页也已接入，并新增农户本人商品列表接口。完整后端 `mvn test` 通过 75 项，前端通过 21 项测试及生产构建。

`.vscode/` 已由 `.gitignore` 忽略，后续不得提交。

最近相关提交：`a773e1e`、`3849014`、`1a3b59b`、`d430e0a`、`eb6ec7c`。

## 最大业务缺口

商品审核主流程的前后端功能已经接通，尚需使用真实 FARMER、ADMIN、USER 账号完成浏览器端到端验收。

目标流程应该是：农户提交，进入待审核，管理员同意后变成 `ON_SALE`，普通用户首页才显示。

管理员页面支持筛选、通过和带原因拒绝；农户页面支持查看本人商品、拒绝原因、重新提交和下架。后端权限仍是最终权限边界。

## P0：明天必须完成

### 1. 商品审核状态和数据库迁移（已完成）

已新增 `backend/src/main/resources/db/migration/V6__product_review.sql`。增加 `PENDING_REVIEW`、`REJECTED` 状态，以及 `reviewed_by`、`reviewed_at`、`review_reason` 字段；历史 `ON_SALE/OFF_SALE` 数据未改写。

已修改 `ProductStatus.java`、`Product.java`、`ProductService.createProduct()` 和农户状态入口；MyBatis 更新策略确保重新提交时审核字段可清空。

规则基础已完成：FARMER 创建或修改商品为 `PENDING_REVIEW`；领域模型支持后续 ADMIN 审核通过为 `ON_SALE` 或拒绝并保存原因；公开商品接口只返回 `ON_SALE`。

### 2. 管理员商品审核 API（已完成）

已新增接口：`GET /api/admin/products/review?status=PENDING_REVIEW`、`POST /api/admin/products/{id}/approve`、`POST /api/admin/products/{id}/reject`。

拒绝接口接收必填 `reason`。只有 ADMIN 可访问；只能审核待审核商品；记录审核人和时间；通过后用户首页可见。管理员审核 API 测试覆盖列表、通过、拒绝、非法状态和权限。

### 3. 农户商品管理页面（已完成）

新增 `frontend/src/views/farmer/FarmerProductsView.vue` 和 `/farmer/products` 路由。

新增 `GET /api/farmer/products`，只返回当前农户自己的商品。页面显示待审核、已上架、已拒绝、已下架状态及拒绝原因，支持重新提交审核、下架和新建入口。已核对 `ProductFormView.vue` 中文正常，无需修改。

### 4. 管理员商品审核页面（已完成）

新增 `frontend/src/views/admin/AdminProductsView.vue` 和 `/admin/products` 路由。

页面已显示商品图片、名称、分类、价格、库存、产地、农户和状态，提供状态筛选、通过、拒绝原因输入、拒绝及加载/错误/空状态。

### 5. 角色测试账号

注册默认只有 `USER`。开发环境可以先在 MySQL 执行：`SELECT id, username FROM users;` 和 `SELECT id, code FROM roles;`，再将目标用户与 `FARMER` 或 `ADMIN` 的角色 ID 插入 `user_roles`。

不能开放普通用户自行注册 ADMIN。

## P1：交易和用户体验

### 1. 商品详情收藏（已完成）

收藏后端已经存在：`GET /api/favorites`、`POST /api/favorites/{productId}`、`DELETE /api/favorites/{productId}`。

`ProductDetailView.vue` 已接入收藏按钮、登录回跳、后端状态初始化、收藏/取消收藏及用户中心 Store 同步，并补充 API 测试。

### 2. 账户安全

后端已有 `PUT /api/user/password` 和 `PUT /api/user/phone`。账户设置页需要增加旧密码、新密码、确认密码、手机号校验、加载状态和成功失败提示。修改密码成功后建议重新登录。

### 3. 结算页地址（已完成）

结算页已调用 `/api/address`，自动选择默认地址，支持切换地址、跳转新增/管理地址，并将选中地址映射到订单收货信息；无常用地址时仍可手工填写。

### 4. 订单完善

补充演示支付或真实支付边界、物流占位页、再次购买、评价入口和订单详情页。

## P2：生产化扩展

- 图片上传：本地上传目录、MinIO 或 OSS，不能把图片二进制存数据库
- 支付：支付单号、支付状态、回调验签、幂等和超时关闭
- 物流：物流单号和状态
- 评价：评分、文字、图片和农户回复
- 售后：退款和售后申请
- 管理员统计：审核数、商品数、用户数、订单数、销售金额、农户排行

## 后续推荐顺序

1. FARMER 提交、ADMIN 审核、USER 首页端到端验证
2. 商品详情收藏按钮
3. 结算页地址选择
4. 密码、手机号、物流、评价

## 验收场景

商品审核：FARMER 从 `/farmer/products/new` 提交；USER 首页看不到；ADMIN 在 `/admin/products` 审核通过；USER 刷新后看到商品；拒绝时 FARMER 能看到原因并重新提交。

收藏：用户在商品详情点击收藏，用户中心出现商品，详情显示已收藏；取消后两处同步消失。

权限：USER 访问管理员接口返回 403；FARMER 不能审核其他农户商品；ADMIN 可以审核所有商品；未登录访问管理页面跳转登录。

## 验证命令

后端：进入 `backend` 执行 `mvn test`；2026-08-19 已通过 75 项，0 failures、0 errors。

前端：进入 `frontend` 执行 `npm run test -- --run` 和 `npm run build`；2026-08-19 已通过 21 项，构建成功。

代码检查：在项目根目录执行 `git diff --check` 和 `git status --short`，确认没有提交 `.vscode/`。

## 启动命令

项目根目录执行 `docker compose up -d`；后端目录执行 `mvn spring-boot:run`；前端目录执行 `npm run dev`。浏览器访问 `http://localhost:5173/`。
