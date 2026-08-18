# 助农商城后续开发交接文档

更新时间：2026-08-19
当前分支：`feature/phase-3-cart-orders`

## 当前状态

项目已经具备真实后端，不是只有前端页面。已完成注册登录、JWT、USER/FARMER/ADMIN 权限、商品目录、农户商品创建/修改/上下架、首页展示、购物车、普通订单、Redis Lua 秒杀、用户中心、收货地址、收藏列表和用户资料接口。

当前未跟踪的 `.vscode/` 目录不要提交。

最近相关提交：`a773e1e`、`3849014`、`1a3b59b`、`d430e0a`、`eb6ec7c`。

## 最大业务缺口

目前实际流程是：农户提交商品，后端直接设置 `ON_SALE`，商品立即出现在普通用户首页。

目标流程应该是：农户提交，进入待审核，管理员同意后变成 `ON_SALE`，普通用户首页才显示。

原因：`ProductService.createProduct()` 当前直接创建 `ON_SALE`；管理员没有商品审核列表页面。

## P0：明天必须完成

### 1. 商品审核状态和数据库迁移

新增 `backend/src/main/resources/db/migration/V6__product_review.sql`。增加 `PENDING_REVIEW`、`REJECTED` 状态，以及 `reviewed_by`、`reviewed_at`、`review_reason` 字段。

修改 `ProductStatus.java`、`Product.java`、`ProductService.createProduct()`。

规则：FARMER 创建商品为 `PENDING_REVIEW`；只有 ADMIN 可以审核通过为 `ON_SALE`；公开商品接口只返回 `ON_SALE`；拒绝时保存原因。

### 2. 管理员商品审核 API

新增接口：`GET /api/admin/products/review?status=PENDING_REVIEW`、`POST /api/admin/products/{id}/approve`、`POST /api/admin/products/{id}/reject`。

拒绝接口接收 `reason`。只有 ADMIN 可访问；只能审核待审核商品；记录审核人和时间；通过后用户首页可见。

### 3. 农户商品管理页面

新增 `frontend/src/views/farmer/FarmerProductsView.vue` 和 `/farmer/products` 路由。

显示农户自己的商品状态：待审核、已上架、已拒绝、已下架；显示拒绝原因；允许修改后重新提交审核。现有 `ProductFormView.vue` 存在历史编码乱码，也要修复为中文。

### 4. 管理员商品审核页面

新增 `frontend/src/views/admin/AdminProductsView.vue` 和 `/admin/products` 路由。

页面显示商品图片、名称、分类、价格、库存、产地、农户、状态；提供通过、拒绝、拒绝原因输入和状态筛选。

### 5. 角色测试账号

注册默认只有 `USER`。开发环境可以先在 MySQL 执行：`SELECT id, username FROM users;` 和 `SELECT id, code FROM roles;`，再将目标用户与 `FARMER` 或 `ADMIN` 的角色 ID 插入 `user_roles`。

不能开放普通用户自行注册 ADMIN。

## P1：交易和用户体验

### 1. 商品详情收藏

收藏后端已经存在：`GET /api/favorites`、`POST /api/favorites/{productId}`、`DELETE /api/favorites/{productId}`。

但 `ProductDetailView.vue` 尚未完整接入收藏按钮。需要实现登录跳转、后端初始化、收藏/取消收藏状态和 Store 测试。

### 2. 账户安全

后端已有 `PUT /api/user/password` 和 `PUT /api/user/phone`。账户设置页需要增加旧密码、新密码、确认密码、手机号校验、加载状态和成功失败提示。修改密码成功后建议重新登录。

### 3. 结算页地址

结算页调用 `/api/address`，自动选择默认地址，允许切换和新增地址，并将选中地址提交到订单。

### 4. 订单完善

补充演示支付或真实支付边界、物流占位页、再次购买、评价入口和订单详情页。

## P2：生产化扩展

- 图片上传：本地上传目录、MinIO 或 OSS，不能把图片二进制存数据库
- 支付：支付单号、支付状态、回调验签、幂等和超时关闭
- 物流：物流单号和状态
- 评价：评分、文字、图片和农户回复
- 售后：退款和售后申请
- 管理员统计：审核数、商品数、用户数、订单数、销售金额、农户排行

## 明天推荐顺序

1. V6 审核状态和数据库迁移
2. 农户创建商品改为待审核
3. 管理员审核 API 和后端测试
4. 管理员审核页面
5. 农户商品状态页面
6. FARMER 提交、ADMIN 审核、USER 首页验证
7. 商品详情收藏按钮
8. 结算页地址选择
9. 密码、手机号、物流、评价

## 验收场景

商品审核：FARMER 从 `/farmer/products/new` 提交；USER 首页看不到；ADMIN 在 `/admin/products` 审核通过；USER 刷新后看到商品；拒绝时 FARMER 能看到原因并重新提交。

收藏：用户在商品详情点击收藏，用户中心出现商品，详情显示已收藏；取消后两处同步消失。

权限：USER 访问管理员接口返回 403；FARMER 不能审核其他农户商品；ADMIN 可以审核所有商品；未登录访问管理页面跳转登录。

## 验证命令

后端：进入 `backend` 执行 `mvn test`。

前端：进入 `frontend` 执行 `npm run test -- --run` 和 `npm run build`。

代码检查：在项目根目录执行 `git diff --check` 和 `git status --short`，确认没有提交 `.vscode/`。

## 启动命令

项目根目录执行 `docker compose up -d`；后端目录执行 `mvn spring-boot:run`；前端目录执行 `npm run dev`。浏览器访问 `http://localhost:5173/`。
