# 助农商城

这是一个面向买家、农户和管理员的农产品电商系统，采用 Spring Boot + Vue 构建。

当前已完成：

- 用户注册登录、JWT 会话和角色权限控制
- 商品目录、农户商品管理和管理员审核
- 购物车、跨农户结算和普通订单流程
- Redis Lua 秒杀、库存预扣、一人一单和失败补偿
- 用户中心、收货地址、商品收藏和账户安全设置
- 农户工作台、管理员后台和买家商城
- 农户商品图片上传（桌面拖拽、点击选择、移动端相册）
- 开发、测试、生产环境配置隔离
- 内置 HMAC 沙箱支付、支付回调幂等和过期关闭

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security、MyBatis Plus、Flyway
- 前端：Vue 3、TypeScript、Composition API、Pinia、Axios、Vite、Vitest
- 数据：MySQL 8、Redis、Redis Lua
- 基础设施：Docker Compose

## 环境要求

- JDK 21
- Node.js 22
- Docker（包含 Docker Compose）

## 本地启动

复制环境变量示例文件，并替换为本地开发值。生成的 `.env` 只用于本机，不能提交到 Git。

```powershell
Copy-Item .env.example .env
docker compose up -d
```

在一个终端启动后端：

```powershell
cd backend
# 本地开发，默认使用 dev profile
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

使用测试环境启动：

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
mvn spring-boot:run
```

生产环境必须使用 `SPRING_PROFILES_ACTIVE=prod`，并显式配置以下变量：

`AGROMALL_DB_URL`、`AGROMALL_DB_USERNAME`、`AGROMALL_DB_PASSWORD`、`AGROMALL_REDIS_HOST`、`AGROMALL_REDIS_PORT`、`AGROMALL_REDIS_PASSWORD`、`AGROMALL_REDIS_KEY_PREFIX`、`AGROMALL_JWT_SECRET`、`AGROMALL_SANDBOX_PAYMENT_SECRET`、`AGROMALL_UPLOAD_PRODUCT_DIR`。

在另一个终端启动前端：

```powershell
cd frontend
npm install
npm run dev
```

默认端口：

- 前端（Vite）：`5173`
- 后端：`8080`
- MySQL: `3306`
- Redis: `6379`

## 测试

运行后端测试：

```powershell
cd backend
mvn clean verify
```

运行前端测试和生产构建：

```powershell
cd frontend
npm run test -- --run
npm run build
```

测试和验收文档：

- [商品目录验收](docs/testing/phase-2-product-catalog.md)
- [购物车与普通订单验收](docs/testing/phase-3-cart-orders.md)
- [秒杀验收](docs/testing/phase-4-seckill.md)

## 支付说明

当前提供的是内置沙箱支付，用于开发和验收，不会产生真实扣款。订单详情中的“模拟支付成功/失败”会调用沙箱接口；真实支付宝、微信支付、退款和支付对账尚未接入。

## 安全注意事项

- `.env` 仅限本地使用，不能提交。
- 项目不提供默认管理员密码；本地需要管理员时，应通过明确的初始化流程创建。
- 客户端请求使用 Bearer token 认证，密码只能发送到登录或注册接口。
- 测试数据库、Redis key 前缀和上传目录与开发环境隔离，生产环境不得使用验收账号和演示数据。
