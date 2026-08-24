# 助农商城用户中心设计

## 目标

在现有 Vue 3、Pinia、Axios、Spring Boot 和 MySQL 架构中增加可运行的用户中心，覆盖订单、收货地址、收藏商品、个人概览和账户设置，并通过顶部用户菜单统一入口。

## 范围与顺序

P0 实现用户中心布局、订单查询/状态筛选/取消/确认收货，以及地址增删改和默认地址设置。P1 实现个人概览统计和最近订单、收藏列表及商品详情收藏切换。P2 实现用户资料、密码和手机号更新。所有页面要求登录后访问，移动端将侧栏变为横向滚动导航。

## 后端设计

新增 Flyway 迁移创建 `user_addresses`、`user_favorites` 表，并为地址默认状态和用户-商品收藏建立唯一约束。新增 `/api/address`、`/api/favorites`、`/api/user/profile`、`/api/user/password`、`/api/user/phone` 接口，沿用现有 `ApiResponse<T>` 的 `code/message/data` 响应结构和 JWT 用户身份。订单接口保留现有路径，同时增加按状态筛选的 `GET /api/orders/my?status=&page=&size=` 能力；状态映射到待付款、待收货、已完成、已取消。

后端服务负责所有权校验、手机号和地址字段校验、默认地址互斥、收藏幂等，以及取消/确认收货的合法状态转换。地址和收藏接口返回面向前端的 View DTO，不暴露数据库实体。

## 前端设计

新增 `UserLayout`、`UserDropdown`、`OrderCard`、`AddressCard`、`AddressForm`、`FavoriteCard`、`StatusBadge`、`StatCard`、`EmptyState` 组件，以及 `UserView`、`UserOrdersView`、`UserAddressView`、`UserFavoritesView`、`UserSettingsView` 页面。新增 `user`、`addresses`、`favorites` API 和 Pinia Store。路由为 `/user`、`/user/orders`、`/user/address`、`/user/favorites`、`/user/settings`，订单状态通过 `status` 查询参数同步。

顶部菜单在登录态显示头像占位、用户名和未读徽标，点击展开用户中心入口；退出操作复用现有认证 Store。订单、地址和收藏操作显示加载状态、错误提示，删除地址和取消订单使用浏览器确认对话框。商品详情页的收藏按钮调用收藏 Store。

## 视觉与响应式

沿用现有 CSS Variables、绿色主色、米白背景和卡片阴影。用户中心采用 200px 侧栏加最大 960px 内容区，卡片圆角 10px。宽度低于 760px 时侧栏改为横向滚动标签，订单卡片和地址操作区堆叠，收藏网格降为两列或一列。

## 验证

新增前端 Store 和组件行为测试，补充后端控制器/服务集成测试，运行前端 `npm run test -- --run`、`npm run build` 和后端 `mvn test`。不修改用户已有的 `.vscode/` 未跟踪目录。
