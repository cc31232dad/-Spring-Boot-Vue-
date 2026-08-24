# Cart and Normal Orders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add authenticated cart, normal checkout, farmer-scoped orders, stock deduction, order status transitions, frontend cart/order pages, and long-term handoff documentation.

**Architecture:** Keep the existing modular monolith. Add focused `cart` and `order` backend packages with MyBatis-Plus mappers and services. Frontend adds API modules, Pinia stores, and pages that reuse the Phase 2 storefront style.

**Tech Stack:** Spring Boot 3.5, Java 21, MyBatis-Plus, Flyway, MySQL, Vue 3, Pinia, Vue Router, Axios, Vitest.

## Global Constraints

- Architecture remains a single Spring Boot + Vue application.
- Checkout succeeds only for authenticated users.
- A cart may contain products from different farmers.
- Checkout groups cart items by product `farmerId` and creates separate orders for each farmer.
- Order creation immediately deducts product stock.
- If any selected item is invalid, off-sale, or understocked, checkout fails and no stock is deducted.
- Order amount is `unit price * quantity`; no shipping fee is calculated in this phase.
- Product image data remains URL strings only.
- Order statuses are exactly `PENDING_SHIPMENT`, `SHIPPED`, `COMPLETED`, `CANCELLED`.
- A shopper may cancel only their own `PENDING_SHIPMENT` order; cancelling restores stock.
- A shopper may complete only their own `SHIPPED` order.
- A farmer may ship only orders assigned to that farmer.
- Admin may view all orders.
- Do not implement Alipay, seckill, Redis stock pre-deduction, logistics, coupons, reviews, refunds, or analytics in this phase.
- Maintain `docs/handoff/project-handoff.md` as a future handoff document.

---

## File map

Backend:

- Create `backend/src/main/resources/db/migration/V3__cart_orders.sql`: cart/order/order-item tables and indexes.
- Create `backend/src/main/java/com/agromall/cart/domain/CartItem.java`: cart item entity.
- Create `backend/src/main/java/com/agromall/cart/infrastructure/CartItemMapper.java`: cart mapper.
- Create `backend/src/main/java/com/agromall/cart/api/*`: cart request/view/controller DTOs.
- Create `backend/src/main/java/com/agromall/cart/application/CartService.java`: cart business logic.
- Create `backend/src/main/java/com/agromall/order/domain/Order.java`, `OrderItem.java`, `OrderStatus.java`: order domain.
- Create `backend/src/main/java/com/agromall/order/infrastructure/OrderMapper.java`, `OrderItemMapper.java`: order mappers.
- Create `backend/src/main/java/com/agromall/order/api/*`: checkout request, views, controller.
- Create `backend/src/main/java/com/agromall/order/application/OrderService.java`: checkout and status transitions.
- Modify `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`: cart/order/stock errors.
- Modify `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`: authenticated route rules for cart/orders/farmer-orders/admin-orders.
- Test `backend/src/test/java/com/agromall/cart/CartApiTest.java`.
- Test `backend/src/test/java/com/agromall/order/OrderCheckoutApiTest.java`.
- Test `backend/src/test/java/com/agromall/order/OrderPermissionApiTest.java`.

Frontend:

- Create `frontend/src/api/cart.ts`, `frontend/src/api/orders.ts`.
- Create `frontend/src/stores/cart.ts`, `frontend/src/stores/cart.spec.ts`.
- Create `frontend/src/stores/orders.ts`, `frontend/src/stores/orders.spec.ts`.
- Modify `frontend/src/views/ProductDetailView.vue`: add add-to-cart quantity/action.
- Create `frontend/src/views/CartView.vue`.
- Create `frontend/src/views/OrderListView.vue`.
- Create `frontend/src/views/farmer/FarmerOrdersView.vue`.
- Create `frontend/src/views/admin/AdminOrdersView.vue`.
- Modify `frontend/src/router/index.ts`: add `/cart`, `/orders`, `/farmer/orders`, `/admin/orders`.
- Modify `frontend/src/styles.css`: cart/order page styles.

Docs:

- Create `docs/testing/phase-3-cart-orders.md`.
- Create `docs/handoff/project-handoff.md`.
- Modify `README.md`.

---

### Task 1: Cart and order schema/domain foundation

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__cart_orders.sql`
- Create: `backend/src/main/java/com/agromall/cart/domain/CartItem.java`
- Create: `backend/src/main/java/com/agromall/cart/infrastructure/CartItemMapper.java`
- Create: `backend/src/main/java/com/agromall/order/domain/Order.java`
- Create: `backend/src/main/java/com/agromall/order/domain/OrderItem.java`
- Create: `backend/src/main/java/com/agromall/order/domain/OrderStatus.java`
- Create: `backend/src/main/java/com/agromall/order/infrastructure/OrderMapper.java`
- Create: `backend/src/main/java/com/agromall/order/infrastructure/OrderItemMapper.java`
- Create: `backend/src/test/java/com/agromall/order/OrderSchemaTest.java`

**Interfaces:**
- Consumes: existing `product`, `users` tables.
- Produces: `cart_items`, `orders`, `order_items`; entities and mappers later services rely on.

- [ ] **Step 1: Write the failing schema test**

Create `OrderSchemaTest` that autowires `CartItemMapper`, `OrderMapper`, `OrderItemMapper`, and `ProductMapper`.

Test behaviors:

```java
@Test
void createsCartOrderAndOrderItemRows() {
    Product product = Product.create(1L, 100L, "订单测试苹果", "香甜苹果",
            new BigDecimal("12.50"), 20, "陕西洛川", "https://example.com/apple.jpg");
    productMapper.insert(product);

    CartItem cartItem = CartItem.create(200L, product.getId(), 2);
    cartItemMapper.insert(cartItem);

    Order order = Order.create("ORD202607190001", 200L, 100L,
            new BigDecimal("25.00"), "张三", "13800000000", "陕西省西安市");
    orderMapper.insert(order);

    OrderItem item = OrderItem.create(order.getId(), product.getId(), "订单测试苹果",
            "https://example.com/apple.jpg", "陕西洛川", new BigDecimal("12.50"), 2);
    orderItemMapper.insert(item);

    assertThat(cartItemMapper.selectById(cartItem.getId()).getQuantity()).isEqualTo(2);
    assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT.name());
    assertThat(orderItemMapper.selectById(item.getId()).getSubtotal()).isEqualByComparingTo("25.00");
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=OrderSchemaTest test
```

Expected: FAIL because schema/domain classes do not exist.

- [ ] **Step 3: Add Flyway migration**

`V3__cart_orders.sql` creates:

```sql
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0),
    INDEX idx_cart_user (user_id),
    INDEX idx_cart_product (product_id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    buyer_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0),
    INDEX idx_orders_buyer (buyer_id),
    INDEX idx_orders_farmer (farmer_id),
    INDEX idx_orders_status (status)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_image_url VARCHAR(500) NOT NULL,
    origin_place VARCHAR(100) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal >= 0),
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
);
```

- [ ] **Step 4: Add domain classes and mappers**

Follow Phase 2 style: Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, MyBatis-Plus `@TableName`, `@TableId(type = IdType.AUTO)`.

`OrderStatus`:

```java
public enum OrderStatus {
    PENDING_SHIPMENT,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
```

`CartItem.create(Long userId, Long productId, Integer quantity)` initializes fields.

`Order.create(String orderNo, Long buyerId, Long farmerId, BigDecimal totalAmount, String receiverName, String receiverPhone, String receiverAddress)` initializes `PENDING_SHIPMENT`.

`OrderItem.create(Long orderId, Long productId, String productName, String productImageUrl, String originPlace, BigDecimal unitPrice, Integer quantity)` calculates `subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))`.

- [ ] **Step 5: Run test and commit**

Run:

```powershell
cd backend
mvn -Dtest=OrderSchemaTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/resources/db/migration/V3__cart_orders.sql backend/src/main/java/com/agromall/cart backend/src/main/java/com/agromall/order backend/src/test/java/com/agromall/order/OrderSchemaTest.java
git commit -m "feat: add cart order schema"
```

---

### Task 2: Cart backend APIs

**Files:**
- Create: `backend/src/main/java/com/agromall/cart/api/CartController.java`
- Create: `backend/src/main/java/com/agromall/cart/api/AddCartItemRequest.java`
- Create: `backend/src/main/java/com/agromall/cart/api/UpdateCartItemRequest.java`
- Create: `backend/src/main/java/com/agromall/cart/api/CartItemView.java`
- Create: `backend/src/main/java/com/agromall/cart/api/CartView.java`
- Create: `backend/src/main/java/com/agromall/cart/application/CartService.java`
- Modify: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/agromall/cart/CartApiTest.java`

**Interfaces:**
- Consumes: Task 1 `CartItem`, `CartItemMapper`; Phase 2 `ProductMapper`, `ProductStatus`.
- Produces: authenticated cart APIs and `CartService` for checkout.

- [ ] **Step 1: Write failing cart API tests**

`CartApiTest` should use existing auth helpers style from prior integration tests.

Cover:

- unauthenticated `GET /api/cart` returns 401;
- authenticated user adds an `ON_SALE` product and sees it in `GET /api/cart`;
- adding same product again updates quantity;
- updating another user's cart item returns 404 or 403;
- deleting cart item removes it;
- adding `OFF_SALE` product returns 409.

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=CartApiTest test
```

Expected: FAIL because cart APIs do not exist.

- [ ] **Step 3: Add error codes**

Extend `ErrorCode`:

```java
CART_ITEM_NOT_FOUND(1008, "Cart item not found"),
PRODUCT_UNAVAILABLE(1009, "Product unavailable"),
INSUFFICIENT_STOCK(1010, "Insufficient stock"),
ORDER_NOT_FOUND(1011, "Order not found"),
INVALID_ORDER_STATUS(1012, "Invalid order status"),
```

Map `PRODUCT_UNAVAILABLE`, `INSUFFICIENT_STOCK`, and `INVALID_ORDER_STATUS` to HTTP `409` in `GlobalExceptionHandler` if the handler uses explicit status mapping.

- [ ] **Step 4: Add DTOs and views**

Requests:

```java
public record AddCartItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
public record UpdateCartItemRequest(@NotNull @Min(1) Integer quantity) {}
```

Views:

```java
public record CartItemView(Long id, Long productId, String productName, BigDecimal price,
                           Integer quantity, Integer stock, String originPlace, String imageUrl,
                           Long farmerId, BigDecimal subtotal) {}
public record CartView(List<CartItemView> items, BigDecimal totalAmount) {}
```

- [ ] **Step 5: Implement `CartService`**

Methods:

```java
CartView getCart(Long userId)
CartView addItem(Long userId, AddCartItemRequest request)
CartView updateItem(Long userId, Long itemId, UpdateCartItemRequest request)
void deleteItem(Long userId, Long itemId)
void clearCart(Long userId)
List<CartItem> getOwnedItems(Long userId, List<Long> itemIds)
void deleteItems(Long userId, List<Long> itemIds)
```

Rules:

- product must exist and `status == ON_SALE`;
- requested quantity must be `<= product.stock` when adding/updating;
- item ownership is always checked by `userId`;
- cart total uses current product price.

- [ ] **Step 6: Implement controller and security**

Routes:

```java
@GetMapping("/api/cart")
@PostMapping("/api/cart/items")
@PutMapping("/api/cart/items/{id}")
@DeleteMapping("/api/cart/items/{id}")
@DeleteMapping("/api/cart")
```

Use `@AuthenticationPrincipal JwtService.JwtPrincipal principal`.

In `SecurityConfig`, require authentication for `/api/cart/**`.

- [ ] **Step 7: Run test and commit**

Run:

```powershell
cd backend
mvn -Dtest=CartApiTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/java/com/agromall/cart backend/src/main/java/com/agromall/common/exception backend/src/main/java/com/agromall/auth/security/SecurityConfig.java backend/src/test/java/com/agromall/cart/CartApiTest.java
git commit -m "feat: add cart APIs"
```

---

### Task 3: Checkout and order backend APIs

**Files:**
- Create: `backend/src/main/java/com/agromall/order/api/CheckoutRequest.java`
- Create: `backend/src/main/java/com/agromall/order/api/OrderItemView.java`
- Create: `backend/src/main/java/com/agromall/order/api/OrderView.java`
- Create: `backend/src/main/java/com/agromall/order/api/OrderController.java`
- Create: `backend/src/main/java/com/agromall/order/application/OrderService.java`
- Modify: `backend/src/main/java/com/agromall/product/infrastructure/ProductMapper.java`
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/agromall/order/OrderCheckoutApiTest.java`

**Interfaces:**
- Consumes: Task 1 order domain/mappers; Task 2 cart service; Phase 2 product mapper.
- Produces: checkout and shopper order APIs.

- [ ] **Step 1: Write failing checkout tests**

`OrderCheckoutApiTest` covers:

- checkout selected cart items creates one order for one farmer;
- checkout cart items from two farmers creates two orders;
- checkout deducts product stock;
- checkout deletes checked-out cart items;
- insufficient stock returns 409/code `1010`;
- insufficient stock leaves stock, cart, and order count unchanged;
- public detail for off-sale product remains unaffected by order snapshots.

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=OrderCheckoutApiTest test
```

Expected: FAIL because order APIs do not exist.

- [ ] **Step 3: Add checkout request and order views**

`CheckoutRequest`:

```java
public record CheckoutRequest(
        @NotEmpty List<Long> cartItemIds,
        @NotBlank @Size(max = 50) String receiverName,
        @NotBlank @Size(max = 30) String receiverPhone,
        @NotBlank @Size(max = 255) String receiverAddress
) {}
```

Views:

```java
public record OrderItemView(Long id, Long productId, String productName, String productImageUrl,
                            String originPlace, BigDecimal unitPrice, Integer quantity,
                            BigDecimal subtotal) {}

public record OrderView(Long id, String orderNo, Long buyerId, Long farmerId, String status,
                        BigDecimal totalAmount, String receiverName, String receiverPhone,
                        String receiverAddress, List<OrderItemView> items) {}
```

- [ ] **Step 4: Add atomic stock update**

In `ProductMapper`, add:

```java
@Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

@Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);
```

- [ ] **Step 5: Implement `OrderService.checkout`**

Signature:

```java
@Transactional
public List<OrderView> checkout(Long buyerId, CheckoutRequest request)
```

Algorithm:

1. load owned cart items using `CartService.getOwnedItems`;
2. if the number of found items differs from requested IDs, throw `CART_ITEM_NOT_FOUND`;
3. load products by ID and validate all are `ON_SALE`;
4. validate every cart quantity is within current stock;
5. group by `product.farmerId`;
6. for each farmer group, create `Order` with generated order number;
7. create `OrderItem` snapshots;
8. call `productMapper.deductStock(productId, quantity)` for every item and require result `1`;
9. delete checked-out cart items;
10. return created order views.

Order number format:

```text
ORD + yyyyMMddHHmmssSSS + six random digits
```

- [ ] **Step 6: Implement shopper order endpoints**

Routes:

```java
POST /api/orders/checkout
GET /api/orders/my
GET /api/orders/{id}
PATCH /api/orders/{id}/cancel
PATCH /api/orders/{id}/complete
```

Cancel:

- only buyer can cancel;
- only `PENDING_SHIPMENT`;
- restore all order item stock;
- set status `CANCELLED`.

Complete:

- only buyer can complete;
- only `SHIPPED`;
- set status `COMPLETED`.

- [ ] **Step 7: Run test and commit**

Run:

```powershell
cd backend
mvn -Dtest=OrderCheckoutApiTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/java/com/agromall/order backend/src/main/java/com/agromall/product/infrastructure/ProductMapper.java backend/src/main/java/com/agromall/auth/security/SecurityConfig.java backend/src/test/java/com/agromall/order/OrderCheckoutApiTest.java
git commit -m "feat: add checkout order APIs"
```

---

### Task 4: Farmer/admin order permissions and status flow

**Files:**
- Modify: `backend/src/main/java/com/agromall/order/api/OrderController.java`
- Modify: `backend/src/main/java/com/agromall/order/application/OrderService.java`
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/agromall/order/OrderPermissionApiTest.java`

**Interfaces:**
- Consumes: Task 3 order APIs and service.
- Produces: farmer/admin order views and farmer ship transition.

- [ ] **Step 1: Write failing permission/status tests**

`OrderPermissionApiTest` covers:

- shopper cannot see another shopper's order;
- farmer sees only orders assigned to their `farmerId`;
- a second farmer cannot ship another farmer's order;
- assigned farmer can ship `PENDING_SHIPMENT` order;
- shopper can complete `SHIPPED` order;
- shopper cannot cancel `SHIPPED` order;
- admin can list all orders.

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=OrderPermissionApiTest test
```

Expected: FAIL for missing farmer/admin endpoints or status checks.

- [ ] **Step 3: Implement farmer/admin endpoints**

Routes:

```java
GET /api/farmer/orders
PATCH /api/farmer/orders/{id}/ship
GET /api/admin/orders
```

Service methods:

```java
List<OrderView> listFarmerOrders(Long farmerId)
OrderView shipOrder(Long farmerId, Long orderId)
List<OrderView> listAdminOrders()
```

Rules:

- farmer order list filters `farmer_id = principal.userId()`;
- ship requires status `PENDING_SHIPMENT`;
- ship sets status `SHIPPED`;
- admin order list returns all orders newest first.

- [ ] **Step 4: Run tests and commit**

Run:

```powershell
cd backend
mvn -Dtest=OrderPermissionApiTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/java/com/agromall/order backend/src/main/java/com/agromall/auth/security/SecurityConfig.java backend/src/test/java/com/agromall/order/OrderPermissionApiTest.java
git commit -m "feat: add order management permissions"
```

---

### Task 5: Frontend cart/order API modules and stores

**Files:**
- Create: `frontend/src/api/cart.ts`
- Create: `frontend/src/api/orders.ts`
- Create: `frontend/src/stores/cart.ts`
- Create: `frontend/src/stores/cart.spec.ts`
- Create: `frontend/src/stores/orders.ts`
- Create: `frontend/src/stores/orders.spec.ts`

**Interfaces:**
- Consumes: backend APIs from Tasks 2-4.
- Produces: frontend stores consumed by pages in Tasks 6-7.

- [ ] **Step 1: Write failing store tests**

`cart.spec.ts`:

- mocks `../api/cart`;
- verifies `loadCart` stores items and total amount;
- verifies `updateQuantity` reloads or updates cart;
- verifies `checkout` is not in cart store; checkout belongs to order store.

`orders.spec.ts`:

- mocks `../api/orders`;
- verifies `checkout` stores returned orders and calls API with receiver info;
- verifies `loadMyOrders` stores order list.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
cd frontend
npm run test -- --run src/stores/cart.spec.ts src/stores/orders.spec.ts
```

Expected: FAIL because modules do not exist.

- [ ] **Step 3: Add cart API and store**

`cart.ts` exports:

```ts
export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  stock: number
  originPlace: string
  imageUrl: string
  farmerId: number
  subtotal: number
}
export interface Cart { items: CartItem[]; totalAmount: number }
export interface AddCartItemPayload { productId: number; quantity: number }
export interface UpdateCartItemPayload { quantity: number }

export async function getCart(): Promise<Cart>
export async function addCartItem(payload: AddCartItemPayload): Promise<Cart>
export async function updateCartItem(id: number, payload: UpdateCartItemPayload): Promise<Cart>
export async function deleteCartItem(id: number): Promise<void>
export async function clearCart(): Promise<void>
```

`useCartStore` state/actions:

```ts
items, totalAmount, loadCart, addItem, updateQuantity, removeItem, clear
```

- [ ] **Step 4: Add order API and store**

`orders.ts` exports:

```ts
export type OrderStatus = 'PENDING_SHIPMENT' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'
export interface CheckoutPayload {
  cartItemIds: number[]
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}
export interface OrderItem {
  id: number
  productId: number
  productName: string
  productImageUrl: string
  originPlace: string
  unitPrice: number
  quantity: number
  subtotal: number
}
export interface Order {
  id: number
  orderNo: string
  buyerId: number
  farmerId: number
  status: OrderStatus
  totalAmount: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  items: OrderItem[]
}

export async function checkout(payload: CheckoutPayload): Promise<Order[]>
export async function listMyOrders(): Promise<Order[]>
export async function getOrder(id: number): Promise<Order>
export async function cancelOrder(id: number): Promise<Order>
export async function completeOrder(id: number): Promise<Order>
export async function listFarmerOrders(): Promise<Order[]>
export async function shipOrder(id: number): Promise<Order>
export async function listAdminOrders(): Promise<Order[]>
```

`useOrderStore` state/actions:

```ts
orders, currentOrder, checkoutCart, loadMyOrders, cancel, complete, loadFarmerOrders, ship, loadAdminOrders
```

- [ ] **Step 5: Run tests/build and commit**

Run:

```powershell
cd frontend
npm run test -- --run src/stores/cart.spec.ts src/stores/orders.spec.ts
npm run build
```

Expected: PASS.

Commit:

```bash
git add frontend/src/api/cart.ts frontend/src/api/orders.ts frontend/src/stores/cart.ts frontend/src/stores/cart.spec.ts frontend/src/stores/orders.ts frontend/src/stores/orders.spec.ts
git commit -m "feat: add cart order frontend stores"
```

---

### Task 6: Product detail add-to-cart and cart checkout page

**Files:**
- Modify: `frontend/src/views/ProductDetailView.vue`
- Create: `frontend/src/views/CartView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Consumes: Task 5 `useCartStore`, `useOrderStore`.
- Produces: shopper add-to-cart and checkout UI.

- [ ] **Step 1: Update product detail**

Add:

- quantity input default `1`, min `1`;
- "加入购物车" button;
- if not logged in, redirect to login with current path as `redirect`;
- on success, route to `{ name: 'cart' }` or show success message with cart link.

- [ ] **Step 2: Add cart route**

Router:

```ts
{ path: '/cart', name: 'cart', component: CartView, meta: { requiresAuth: true } }
```

- [ ] **Step 3: Build cart page**

`CartView`:

- loads cart on mount;
- renders item cards/table;
- supports quantity update and delete;
- receiver form: `receiverName`, `receiverPhone`, `receiverAddress`;
- checkout button calls `orderStore.checkoutCart({ cartItemIds: cart.items.map(i => i.id), receiverName, receiverPhone, receiverAddress })`;
- on checkout success, reload cart and route to `{ name: 'orders' }`;
- empty cart shows link back home.

- [ ] **Step 4: Add styles**

Use existing warm storefront classes. Add classes:

```css
.cart-shell
.cart-list
.cart-item
.quantity-control
.checkout-panel
.receiver-form
```

- [ ] **Step 5: Run build and commit**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/ProductDetailView.vue frontend/src/views/CartView.vue frontend/src/router/index.ts frontend/src/styles.css
git commit -m "feat: add cart checkout page"
```

---

### Task 7: Shopper, farmer, and admin order pages

**Files:**
- Create: `frontend/src/views/OrderListView.vue`
- Create: `frontend/src/views/farmer/FarmerOrdersView.vue`
- Create: `frontend/src/views/admin/AdminOrdersView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Consumes: Task 5 `useOrderStore`.
- Produces: order management pages.

- [ ] **Step 1: Add routes**

```ts
{ path: '/orders', name: 'orders', component: OrderListView, meta: { requiresAuth: true } },
{ path: '/farmer/orders', name: 'farmer-orders', component: FarmerOrdersView, meta: { requiresAuth: true } },
{ path: '/admin/orders', name: 'admin-orders', component: AdminOrdersView, meta: { requiresAuth: true } }
```

- [ ] **Step 2: Add shopper orders page**

Shows:

- order number;
- status label;
- total amount;
- receiver info;
- item snapshots;
- cancel button when `PENDING_SHIPMENT`;
- complete button when `SHIPPED`.

- [ ] **Step 3: Add farmer orders page**

Shows farmer orders and item snapshots. Button:

- `ship` when status is `PENDING_SHIPMENT`.

- [ ] **Step 4: Add admin orders page**

Simple read-only list of all orders with buyer/farmer/status/amount/items.

- [ ] **Step 5: Run build and commit**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/OrderListView.vue frontend/src/views/farmer/FarmerOrdersView.vue frontend/src/views/admin/AdminOrdersView.vue frontend/src/router/index.ts frontend/src/styles.css
git commit -m "feat: add order management pages"
```

---

### Task 8: Phase 3 acceptance tests, docs, and handoff

**Files:**
- Create: `backend/src/test/java/com/agromall/order/OrderFlowIntegrationTest.java`
- Create: `docs/testing/phase-3-cart-orders.md`
- Create: `docs/handoff/project-handoff.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: all Phase 3 APIs and frontend routes.
- Produces: final Phase 3 acceptance evidence and handoff documentation.

- [ ] **Step 1: Write backend flow test**

`OrderFlowIntegrationTest` should:

- register/login a shopper;
- seed two farmers and two products;
- add both products to shopper cart;
- checkout both cart items with receiver info;
- assert two orders were created because products belong to different farmers;
- assert stock was deducted;
- farmer ships one order;
- shopper completes shipped order;
- shopper cancels the other pending order;
- assert cancelled order stock was restored.

- [ ] **Step 2: Run backend acceptance**

Run:

```powershell
cd backend
mvn -Dtest=OrderFlowIntegrationTest test
```

Expected: PASS.

- [ ] **Step 3: Write Phase 3 testing doc**

Create `docs/testing/phase-3-cart-orders.md`:

```markdown
# Phase 3 cart and normal orders acceptance

Test date: 2026-07-19

## Automated commands

```powershell
cd backend
mvn clean verify

cd ..\frontend
npm run test -- --run
npm run build
```

## Manual checklist

1. Login as a normal user.
2. Open product detail and add a product to cart.
3. Open cart and update quantity.
4. Checkout with receiver information.
5. Confirm stock is reduced.
6. Confirm cart item is removed after checkout.
7. Open my orders and see the new order.
8. Login as farmer and see the assigned order.
9. Farmer marks order as shipped.
10. User confirms receipt and order becomes completed.
11. Try ordering more than stock and confirm it fails cleanly.
```
```

- [ ] **Step 4: Write handoff document**

Create `docs/handoff/project-handoff.md` with sections:

```markdown
# Agricultural Mall project handoff

## Overview

Spring Boot 3.5 + Vue 3 agricultural assistance mall.

## Local environment

- JDK 21
- MySQL password used locally: `123456`
- Redis password used locally: `123456`
- `.env` is local-only and must not be committed.
- In a new Windows CMD, run:

```bat
set JAVA_HOME=E:\java21
set PATH=%JAVA_HOME%\bin;%PATH%
```

## Phase 1: auth and RBAC

Completed: registration, login, JWT session recovery, role-based API access.
Deferred: password reset, profile editing.
Problems encountered: JAVA_HOME not set in new CMD; early tests needed security status fixes.
Verification: `mvn clean verify`, `npm run test -- --run`, `npm run build`.

## Phase 2: product catalog

Completed: product schema, fixed categories, public browsing APIs, farmer/admin product management, frontend catalog pages.
Deferred: upload storage, audit workflow, seckill.
Problems encountered: Maven/esbuild sandbox access problems in Codex; normal CMD succeeded.
Verification: see `docs/testing/phase-2-product-catalog.md`.

## Phase 3: cart and normal orders

Completed: cart, checkout, stock deduction, farmer-scoped orders, shopper/farmer/admin order pages.
Deferred: payment, seckill, logistics, refunds, coupons, reviews.
Problems encountered: document concrete issues found during implementation.
Verification: see `docs/testing/phase-3-cart-orders.md`.

## Recommended next phases

1. Phase 4: seckill with Redis stock pre-deduction.
2. Phase 5: Alipay sandbox payment.
3. Phase 6: logistics, reviews, admin statistics, and UI polish.
```

- [ ] **Step 5: Update README**

Add Phase 3 testing doc link in README tests section and mention cart/orders in the opening description.

- [ ] **Step 6: Run full verification and commit**

Run:

```powershell
cd backend
mvn clean verify

cd ..\frontend
npm run test -- --run
npm run build
```

Expected:

- backend `BUILD SUCCESS`;
- frontend tests passed;
- frontend build succeeded.

Commit:

```bash
git add backend/src/test/java/com/agromall/order/OrderFlowIntegrationTest.java docs/testing/phase-3-cart-orders.md docs/handoff/project-handoff.md README.md
git commit -m "test: verify cart order phase"
```

## Plan self-review

- Spec coverage: schema, cart, checkout, cross-farmer split orders, stock deduction, status transitions, frontend cart/order pages, tests, acceptance docs, and handoff docs are covered.
- Deferred scope remains out: payment, seckill, logistics, coupons, reviews, refunds, analytics.
- Type consistency: backend uses `CartItem`, `Order`, `OrderItem`, `OrderStatus`, `CartView`, `OrderView`, `CheckoutRequest`; frontend uses `Cart`, `CartItem`, `Order`, `OrderItem`, `CheckoutPayload`.
