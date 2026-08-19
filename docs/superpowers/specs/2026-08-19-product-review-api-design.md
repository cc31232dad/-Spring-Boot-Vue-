# 管理员商品审核 API 设计

## 目标

为 P0 商品审核流程提供后端管理员入口：管理员可以按状态查看商品、审核通过或拒绝待审核商品；审核结果记录审核人、时间和拒绝原因，并立即影响公开商品目录。

## 接口

- `GET /api/admin/products/review?status=PENDING_REVIEW`
  - `status` 可选，默认 `PENDING_REVIEW`。
  - 只接受 `PENDING_REVIEW`、`ON_SALE`、`REJECTED`、`OFF_SALE`。
  - 返回 `ProductReviewView` 列表，包含商品详情字段、农户 ID、状态和审核字段。
- `POST /api/admin/products/{id}/approve`
  - 仅允许当前状态为 `PENDING_REVIEW`。
  - 记录当前管理员 ID 和 `LocalDateTime.now()`，状态改为 `ON_SALE`。
- `POST /api/admin/products/{id}/reject`
  - 请求体为 `{\"reason\":\"...\"}`，原因必须非空且不超过 500 个字符。
  - 仅允许当前状态为 `PENDING_REVIEW`。
  - 记录管理员、时间和去除首尾空格后的原因，状态改为 `REJECTED`。

所有接口由现有 Spring Security `/api/admin/**` 规则保护。普通用户、农户和未登录请求分别得到 403 或 401。

## 实现边界

- 在 `ProductService` 中增加管理员审核列表、通过和拒绝用例，复用 `Product.approve/reject` 状态方法。
- 新增 `AdminProductReviewController`、`ProductReviewView` 和 `ProductRejectRequest`，不把数据库实体直接作为响应。
- 新增 `ErrorCode.PRODUCT_REVIEW_INVALID`，非法状态返回 409。
- 不在本阶段实现前端页面、分页、批量审核或审核历史表。

## 测试

- ADMIN 能查询待审核商品、通过商品并使公开详情可见。
- ADMIN 能拒绝商品并持久化原因；空原因返回 400。
- 已审核商品不能重复通过或拒绝，返回 409。
- USER、FARMER、未登录访问审核 API 无法执行管理员操作。
- 不同管理员审核时持久化实际审核人和时间。
