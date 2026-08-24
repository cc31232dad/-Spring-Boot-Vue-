# 商品审核状态与 V6 迁移设计

## 目标

为商品审核流程建立数据库和领域模型基础。农户新建或重新编辑商品后必须进入 `PENDING_REVIEW`，不得直接上架；公开商城继续只展示 `ON_SALE` 商品。

## 范围

- 新增商品状态 `PENDING_REVIEW` 和 `REJECTED`，保留 `ON_SALE`、`OFF_SALE`。
- 新增 Flyway V6 迁移，为 `product` 增加 `reviewed_by`、`reviewed_at`、`review_reason`。
- `reviewed_by` 使用可空外键关联 `users.id`；审核字段允许为空，以支持待审核商品和兼容历史数据。
- 历史商品状态不批量改写，已有 `ON_SALE/OFF_SALE` 商品保持原状态。
- 商品实体提供待审核、通过、拒绝和下架的明确状态转换方法。
- 本阶段不新增管理员审核 HTTP API、管理页面或农户商品列表页面，这些属于后续 P0 项。

## 状态规则

- 农户创建商品：`PENDING_REVIEW`，审核字段为空。
- 农户修改自己商品：更新业务字段后重新变为 `PENDING_REVIEW`，并清空旧审核人、审核时间和拒绝原因。
- 农户调用现有 `/api/farmer/products/{id}/on-sale`：兼容保留接口，但语义改为重新提交审核，结果为 `PENDING_REVIEW`。
- 农户或管理员下架商品：变为 `OFF_SALE`；审核记录保留，便于追踪最后一次审核。
- 管理员通过：变为 `ON_SALE`，记录审核人和时间，清空拒绝原因。
- 管理员拒绝：变为 `REJECTED`，记录审核人、时间和非空拒绝原因。
- 公开列表和详情仍只查询 `ON_SALE`，因此待审核和已拒绝商品不会暴露给普通用户。

## 数据模型

`Product` 增加：

- `Long reviewedBy`
- `LocalDateTime reviewedAt`
- `String reviewReason`

V6 新增同名蛇形字段，并为 `reviewed_by` 添加普通索引和用户外键。`review_reason` 使用 `VARCHAR(500)`，足以保存管理员说明且避免无界文本。

## 测试

- API 测试证明农户创建商品返回 `PENDING_REVIEW`，公开列表和详情均不可见。
- API 测试证明农户修改被拒商品后状态回到 `PENDING_REVIEW` 且审核信息清空。
- API 测试证明农户不能通过现有 on-sale 接口直接恢复 `ON_SALE`。
- Schema 测试证明 V6 字段可持久化和读取，并证明新商品默认状态为 `PENDING_REVIEW`。
- 现有需要可交易商品的购物车、订单、秒杀和用户中心测试显式调用管理员通过领域方法，避免测试夹具依赖“创建即上架”的旧规则。

