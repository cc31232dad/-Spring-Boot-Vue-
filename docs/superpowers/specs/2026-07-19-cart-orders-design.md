# Phase 3 cart and normal orders design

Date: 2026-07-19

## Goal

Phase 3 adds a complete normal shopping flow for the agricultural mall:

- shoppers add products to a cart;
- shoppers update or remove cart items;
- shoppers checkout selected cart items;
- the backend creates one order per farmer when the cart contains products from multiple farmers;
- product stock is deducted immediately after successful order creation;
- shoppers, farmers, and admins can view the orders they are allowed to see;
- the project handoff document records completed work, deferred work, and known problems for future continuation.

This phase deliberately excludes payment, flash sale, logistics tracking, coupons, reviews, refunds, and after-sales flows. Those are later phases.

## Confirmed business rules

- Architecture remains a single Spring Boot + Vue application.
- Checkout succeeds only for authenticated users.
- A cart may contain products from different farmers.
- Checkout groups cart items by product `farmerId` and creates separate orders for each farmer.
- Order creation immediately deducts product stock.
- If any selected item is invalid, off-sale, or understocked, checkout fails and no stock is deducted.
- Order amount is `unit price * quantity`; no shipping fee is calculated in this phase.
- Product image data remains URL strings only.
- Orders use these statuses:
  - `PENDING_SHIPMENT`: order created and waiting for farmer shipment.
  - `SHIPPED`: farmer has shipped the order.
  - `COMPLETED`: shopper confirmed receipt or admin completed it.
  - `CANCELLED`: order cancelled.
- A shopper may cancel only their own `PENDING_SHIPMENT` order.
- Cancelling a `PENDING_SHIPMENT` order restores stock.
- A shopper may complete only their own `SHIPPED` order.
- A farmer may ship only orders assigned to that farmer.
- An admin may view all orders and may manage order status for support/testing.

## Data model

### `cart_items`

Stores the current user's shopping cart.

Fields:

- `id`
- `user_id`
- `product_id`
- `quantity`
- `created_at`
- `updated_at`

Constraints:

- `quantity > 0`
- unique key on `(user_id, product_id)` so adding the same product increases or replaces quantity instead of creating duplicates.

### `orders`

Stores one farmer-scoped order.

Fields:

- `id`
- `order_no`
- `buyer_id`
- `farmer_id`
- `status`
- `total_amount`
- `receiver_name`
- `receiver_phone`
- `receiver_address`
- `created_at`
- `updated_at`

Constraints:

- `order_no` is unique.
- `status` is one of `PENDING_SHIPMENT`, `SHIPPED`, `COMPLETED`, `CANCELLED`.
- `total_amount >= 0`.

### `order_items`

Stores product snapshots at checkout time.

Fields:

- `id`
- `order_id`
- `product_id`
- `product_name`
- `product_image_url`
- `origin_place`
- `unit_price`
- `quantity`
- `subtotal`
- `created_at`

Why snapshots: later product edits should not change historical order content.

## Backend modules

Add packages under `com.agromall.order` and `com.agromall.cart` or a combined `com.agromall.order` package if implementation stays smaller. Keep boundaries clear:

- cart API and service manage cart state;
- order API and service manage checkout and order state transitions;
- product mapper/service are reused for product lookup and stock checks.

### Cart APIs

- `GET /api/cart`
  - Returns the current user's cart items with product summary data.
- `POST /api/cart/items`
  - Body: `productId`, `quantity`.
  - Adds an on-sale product to cart.
  - If the same product already exists, update quantity.
- `PUT /api/cart/items/{id}`
  - Body: `quantity`.
  - Updates the current user's cart item quantity.
- `DELETE /api/cart/items/{id}`
  - Deletes one current-user cart item.
- `DELETE /api/cart`
  - Clears the current user's cart.

Validation:

- product must exist and be `ON_SALE`;
- quantity must be positive;
- update/delete must belong to the current user.

### Order APIs

- `POST /api/orders/checkout`
  - Body: selected cart item IDs and receiver information.
  - Creates one or more farmer-scoped orders.
  - Deducts stock immediately.
  - Removes checked-out cart items after success.
- `GET /api/orders/my`
  - Current shopper's orders.
- `GET /api/orders/{id}`
  - Current shopper may see their order.
  - Farmer may see an order assigned to them.
  - Admin may see any order.
- `PATCH /api/orders/{id}/cancel`
  - Shopper cancels their `PENDING_SHIPMENT` order and stock is restored.
- `PATCH /api/orders/{id}/complete`
  - Shopper completes their `SHIPPED` order.
- `GET /api/farmer/orders`
  - Farmer sees orders assigned to them.
- `PATCH /api/farmer/orders/{id}/ship`
  - Farmer marks their `PENDING_SHIPMENT` order as `SHIPPED`.
- `GET /api/admin/orders`
  - Admin sees all orders.

Admin status operations may be added only if needed by tests or manual operation. Avoid building a full order management console in this phase.

## Stock and transaction rules

Checkout must run in one database transaction:

1. load selected cart items for the current user;
2. validate products exist and are `ON_SALE`;
3. validate requested quantity is available;
4. group items by `farmerId`;
5. create one order per farmer and order item snapshots;
6. deduct stock for every product;
7. delete checked-out cart items;
8. commit.

If any validation or write fails, the whole checkout rolls back. This prevents half-created orders and partial stock deductions.

For this graduation project, database transaction isolation and an atomic stock update are sufficient. Redis stock locking is deferred to the seckill phase.

## Error handling

Reuse the existing `ApiResponse`, `BusinessException`, and `ErrorCode` pattern.

Add or reuse error codes for:

- cart item not found;
- order not found;
- invalid order status transition;
- insufficient stock;
- product unavailable/off-sale;
- forbidden order/cart access.

HTTP status expectations:

- validation errors: `400`;
- forbidden access: `403`;
- not found: `404`;
- invalid status transition or insufficient stock: `409`.

## Frontend design

Use the current warm agricultural storefront style from Phase 2.

### Product detail

Add a quantity input and "加入购物车" action. The user must be logged in to add to cart. If not logged in, send them to login.

### Cart page `/cart`

Shows:

- cart item list;
- product image/name/origin/price;
- quantity controls;
- remove action;
- selected total amount;
- receiver name/phone/address fields for checkout;
- checkout button.

The first version may checkout all cart items. Selective checkout can be added if implementation remains simple; otherwise it is deferred.

### My orders page `/orders`

Shows shopper orders with:

- order number;
- farmer/order status;
- total amount;
- item snapshots;
- cancel button for `PENDING_SHIPMENT`;
- confirm receipt button for `SHIPPED`.

### Farmer orders page `/farmer/orders`

Shows farmer-owned orders and lets the farmer mark `PENDING_SHIPMENT` orders as shipped.

### Admin orders page `/admin/orders`

Simple read-only all-order list is enough unless status management is needed for acceptance.

## Handoff document

Create and maintain:

```text
docs/handoff/project-handoff.md
```

The document must be friendly to a future maintainer and include:

- project overview and tech stack;
- current branch/phase workflow;
- Phase 1 completed work, deferred work, verification commands, and problems encountered;
- Phase 2 completed work, deferred work, verification commands, and problems encountered;
- Phase 3 completed work, deferred work, verification commands, and problems encountered;
- local environment notes: Java 21, MySQL/Redis password `123456` for local development, `.env` is ignored and must not be committed;
- recurring problems encountered so far:
  - new CMD windows may lose `JAVA_HOME`;
  - Maven can fail when `JAVA_HOME` is not set;
  - tests may require MySQL/Redis services;
  - Codex sandbox previously had Maven/esbuild access issues, while normal CMD succeeded;
  - GitHub work is pushed through feature branches and PRs;
- recommended future phases:
  - Phase 4: seckill with Redis stock pre-deduction;
  - Phase 5: Alipay sandbox payment;
  - Phase 6: logistics, reviews, admin statistics, and polish.

## Tests and acceptance

### Backend tests

Add focused tests for:

- cart add/list/update/delete/clear;
- adding off-sale or missing product fails;
- checkout deducts stock and clears cart items;
- cross-farmer checkout creates multiple orders;
- insufficient stock fails without partial deduction;
- shopper can see own orders only;
- farmer sees only assigned orders;
- admin sees all orders;
- cancel restores stock;
- ship and complete transitions follow the status rules.

### Frontend tests

Add store tests for:

- cart loading and quantity update;
- checkout success clears cart state or reloads cart;
- order list loading.

### Acceptance docs

Create:

```text
docs/testing/phase-3-cart-orders.md
```

Include:

- backend full verification command;
- frontend test/build commands;
- manual checklist for cart, checkout, stock deduction, shopper orders, farmer shipping, and order completion.

## Manual acceptance checklist

1. Login as a normal user.
2. Open product detail and add a product to cart.
3. Open cart and update quantity.
4. Checkout with receiver information.
5. Confirm stock is reduced.
6. Confirm cart item is removed after checkout.
7. Open "my orders" and see the new order.
8. Login as farmer and see the assigned order.
9. Farmer marks order as shipped.
10. User confirms receipt and order becomes completed.
11. Try ordering more than stock and confirm it fails cleanly.

## Deferred scope

- Alipay sandbox payment.
- Flash sale/seckill.
- Redis stock pre-deduction.
- Shipping fee and logistics tracking.
- Coupons and promotions.
- Refund and after-sales.
- Product reviews.
- Advanced admin analytics.

