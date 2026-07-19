# Phase 2 Product Catalog Design

Date: 2026-07-19

## Goal

Phase 2 adds the first real mall business capability to the agricultural mall: product categories, product listing, product detail, and farmer/admin product publishing. It builds on Phase 1 authentication and RBAC without adding cart, order, payment, or seckill behavior yet.

The goal is to make the system feel like a real助农商城:

- Visitors and ordinary users can browse on-sale agricultural products.
- Farmers can publish and manage their own products.
- Admins can manage all products.
- Product stock is recorded and displayed, but stock deduction is left for later order and seckill phases.

## Scope

Included:

- Fixed seeded product categories.
- Product database schema.
- Public category, product list, and product detail APIs.
- Farmer/admin product create and update APIs.
- Product on-sale/off-sale status management.
- Vue mall home page with search, category filtering, and product cards.
- Vue product detail page.
- Simple farmer product form for creating and editing products.
- Automated backend and frontend tests for the main flows.

Excluded for this phase:

- Shopping cart.
- Order creation.
- Payment.
- Stock deduction.
- Seckill.
- Image upload.
- Admin category CRUD.
- Product review/audit workflow.

## Product Categories

Categories are fixed seed data managed by Flyway:

1. 水果
2. 蔬菜
3. 粮油
4. 禽蛋肉类
5. 茶叶特产

Phase 2 does not provide category management screens. This keeps the implementation focused while still giving the UI a real mall structure.

## Product Fields

The `product` table stores:

- `id`
- `category_id`
- `farmer_id`
- `name`
- `description`
- `price`
- `stock`
- `origin_place`
- `image_url`
- `status`
- `created_at`
- `updated_at`

Field rules:

- `price` uses `DECIMAL`, mapped to Java `BigDecimal`.
- `stock` must be greater than or equal to 0.
- `image_url` stores a URL or static path, not binary image data.
- `status` is either `ON_SALE` or `OFF_SALE`.
- `farmer_id` points to the user who published the product.

## Image Strategy

Phase 2 stores only `imageUrl` in the database. The actual image file is loaded by the browser from that URL.

Example API response:

```json
{
  "id": 1,
  "name": "陕西红富士苹果",
  "price": 29.90,
  "stock": 100,
  "originPlace": "陕西洛川",
  "imageUrl": "https://example.com/apple.jpg"
}
```

Vue renders the image with:

```html
<img :src="product.imageUrl" />
```

This mirrors common real-world mall architecture: MySQL stores product metadata and image addresses, while image files live in static hosting, object storage, CDN, or a later upload directory.

## Permissions

Public access:

- `GET /api/categories`
- `GET /api/products`
- `GET /api/products/{id}`

Authenticated role access:

- `FARMER` can create products and manage products where `farmer_id` equals their current user id.
- `ADMIN` can create and manage all products.
- `USER` cannot create or manage products.

Visibility:

- Public product list and detail only expose `ON_SALE` products.
- `OFF_SALE` products are hidden from public browsing.
- Farmer/admin management APIs may access off-sale products when authorized.

## Backend API Design

Public APIs:

```text
GET /api/categories
GET /api/products?keyword=苹果&categoryId=1
GET /api/products/{id}
```

Management APIs:

```text
POST /api/farmer/products
PUT /api/farmer/products/{id}
PATCH /api/farmer/products/{id}/off-sale
PATCH /api/farmer/products/{id}/on-sale
```

List query rules:

- `keyword` performs fuzzy search on product name.
- `categoryId` filters by category.
- Only `ON_SALE` products are returned.
- Default sorting is newest first.

Product create/update request fields:

- `categoryId`
- `name`
- `description`
- `price`
- `stock`
- `originPlace`
- `imageUrl`

## Frontend Design

The current authenticated home page becomes the mall home page:

- Header with current user session and logout.
- Search box for product keyword.
- Category filter buttons.
- Product card grid.
- Card content: image, name, price, stock, origin place.
- Empty state when no product matches.

New pages:

- Product detail page:
  - image
  - name
  - category
  - price
  - stock
  - origin place
  - description
- Farmer product form:
  - create product
  - edit product
  - on-sale/off-sale actions

The form can stay simple in Phase 2. It is acceptable for users with `FARMER` or `ADMIN` roles to access it through a basic route instead of a full management dashboard.

## Error Handling

Use the existing unified API response envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Expected errors:

- Product not found: business error mapped to a not-found style response.
- Category not found: validation/business error.
- Ordinary `USER` attempts to publish product: `403`.
- Farmer edits another farmer's product: `403`.
- Invalid product name, price, stock, or image URL: `400`.

If Phase 1 error codes do not yet contain product-specific codes, Phase 2 should extend `ErrorCode` in a small, explicit way.

## Testing Strategy

Backend tests:

- Category list returns seeded categories.
- Public product list returns only `ON_SALE` products.
- Public product list supports keyword and category filters.
- Public product detail returns an on-sale product.
- Off-sale product is hidden from public detail/list.
- Farmer can create a product.
- Ordinary user cannot create a product.
- Farmer cannot edit another farmer's product.
- Admin can manage any product.
- Invalid price or stock returns validation error.

Frontend tests:

- Product API client maps category/list/detail responses.
- Home/product store can load products and categories.
- Basic product card rendering test if practical.

Manual acceptance:

1. Start backend and frontend.
2. Log in as a farmer/admin test account.
3. Create a product with category, price, stock, origin place, and image URL.
4. Return to home page and confirm the product card appears.
5. Search by keyword and filter by category.
6. Open product detail.
7. Take product off sale and confirm it disappears from public listing.

## Future Extensions

Phase 2 intentionally leaves these for later:

- Cart and order modules will consume `product.id`, `price`, and `stock`.
- Order creation will deduct normal stock.
- Seckill will use Redis and Lua to do high-concurrency stock pre-deduction.
- Image upload can later write files to local storage or object storage and then save the returned URL to `image_url`.
- Admin category management can later replace fixed Flyway seed data.
