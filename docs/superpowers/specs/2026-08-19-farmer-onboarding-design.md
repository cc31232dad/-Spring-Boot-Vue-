# 农户独立资料与多角色认证设计

## 目标

在保留现有 Spring Boot、Vue、JWT 和 `user_roles` 权限模型的前提下，补齐买家、农户、管理员三类认证体验，并实现农户入驻申请、审核、拒绝和重新提交闭环。

## 数据边界

`users` 只保存登录身份和基础账号信息，角色继续由 `roles` 与 `user_roles` 管理。新增 `farmer_profiles` 一对一保存农户资质，新增 `farmer_audits` 保存每次审核记录。农户申请状态保存在 `farmer_profiles.status`，账号封禁继续复用 `users.enabled`。

状态规则：`PENDING` 不能登录农户功能，`APPROVED` 可以登录，`REJECTED` 可重新提交，`users.enabled=false` 时任何角色都不能登录。

## 接口边界

保留现有 `POST /api/auth/register` 和 `POST /api/auth/login` 合同，避免破坏现有调用方。买家继续使用原注册接口；新增 `POST /api/auth/farmer/apply`、`GET /api/admin/farmers`、`GET /api/admin/farmers/{id}`、`POST /api/admin/farmers/{id}/approve` 和 `POST /api/admin/farmers/{id}/reject`。公开接口不接受 ADMIN 注册。

农户登录使用手机号，待审核、拒绝、封禁分别返回稳定业务错误。JWT 继续携带用户 ID、用户名和角色集合，不另建第二套会话机制。

## 前端边界

登录页使用买家/农户 Tab，管理员仅显示内部通道；注册页使用买家/农户 Tab。农户提示采用现有主题中的暖色系，买家继续使用绿色。路由继续由 Vue Router 守卫改善体验，后端权限是最终边界。管理员农户审核页复用现有管理员页面结构，不重做后台壳层。

## 兼容与风险

- 旧买家注册、旧用户名登录和现有测试保持兼容。
- 不将农户字段追加到 `users`，不迁移到 Express，不重建认证表。
- 地区三级联动先使用前端静态数据，通知功能暂不伪造，只记录审核状态和原因。
- 每个后端状态转换覆盖成功、重复申请、非法状态、权限不足和资源所有权边界。

## 验证

先补后端集成测试和前端表单/API 测试，观察红灯后实现。完成后运行后端 `mvn test`、前端 Vitest、生产构建和 `git diff --check`。
