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
Problems encountered: `JAVA_HOME` was not set in a new CMD; early tests needed security status fixes.
Verification: `mvn clean verify`, `npm run test -- --run`, `npm run build`.

## Phase 2: product catalog

Completed: product schema, fixed categories, public browsing APIs, farmer/admin product management, frontend catalog pages.
Deferred: upload storage, audit workflow, seckill.
Problems encountered: Maven/esbuild sandbox access problems in Codex; normal CMD succeeded.
Verification: see `docs/testing/phase-2-product-catalog.md`.

## Phase 3: cart and normal orders

Completed: cart, checkout, stock deduction, farmer-scoped orders, shopper/farmer/admin order pages, acceptance test and testing handoff.
Deferred: payment, seckill, logistics, refunds, coupons, reviews.
Problems encountered: early test helpers used regular expressions to extract `"id"` from nested JSON responses. This was fragile because checkout responses include both order IDs and order-item IDs, and cart add responses include the whole cart list. Helpers were replaced with Jackson JSON tree traversal: checkout reads the first order ID directly and cart item lookup matches by `productId`. Maven also reports Mockito dynamic-agent warnings on JDK 21; current tests still pass.
Verification: see `docs/testing/phase-3-cart-orders.md`; latest full checks passed on 2026-07-21.

## Phase 4: Redis seckill

Implemented: seckill activity schema, Redis stock warmup, Lua atomic reservation, one-user-one-order protection, seckill order creation, Redis compensation, cancellation and timeout recovery, and frontend activity/rush page.
Deferred: real payment integration and production Redis concurrency verification.
Verification: see `docs/testing/phase-4-seckill.md`.

## Phase 7: Product review foundation

Implemented: Flyway V6 product review audit columns, `PENDING_REVIEW` and `REJECTED` domain states, farmer create/edit/resubmit transitions, review audit persistence, and public visibility protection. Existing sale-dependent tests now explicitly approve their fixtures. Full backend verification on 2026-08-19: 77 tests passed.
Completed: administrator review page, farmer-owned product list API, and farmer product status page. Deferred: browser-based FARMER -> ADMIN -> USER acceptance flow.

Administrator review API completed on 2026-08-19: `GET /api/admin/products/review`, `POST /api/admin/products/{id}/approve`, and `POST /api/admin/products/{id}/reject`. ADMIN-only access, pending-state guard, reviewer audit fields, reject reason validation, and public visibility are covered by integration tests.

## Phase 9: Separate role workspaces

Completed on 2026-08-20: the single Vue application now presents three role-specific surfaces. Buyers remain in the public mall at `/`; approved farmers land on `/farmer` with their own navigation for products and orders; administrators land on `/admin` with separate review and order navigation. Farmer and administrator dashboards compose existing APIs with partial-failure handling, and route contracts plus dashboard behavior are covered by frontend tests. `@lucide/vue` supplies the workspace navigation icons. Browser acceptance also verified desktop `1440x900` and mobile `390x844` layouts; dashboard metric cards, task strips, recent-order panels, warnings, and responsive grids are now styled. No new backend API, table, operator role, customer-service role, super-administrator role, or administrator-creation flow was added.

## Recommended next phases

1. Phase 5: Alipay sandbox payment.
2. Run browser regression for all three role workspaces at desktop and mobile sizes, then add order details, logistics placeholders, and repurchase flows.
3. Phase 7: logistics, reviews, admin statistics, and UI polish.

Account security frontend completed on 2026-08-19: profile, phone, and password forms have independent validation and feedback; password changes clear the current client session and require login with the new password. Wrong current passwords use business code 1024/HTTP 400 so they do not trigger JWT-expiry logout, and phone validation requires a `1[3-9]` prefix. Frontend verification: 25 tests passed and production build succeeded. Backend verification: 77 tests passed. Stateless JWTs are not revoked server-side after password changes; add token-version or revocation support before production.

Order detail frontend completed on 2026-08-19: order detail route/page, logistics placeholder, and repurchase-to-cart flow.

Payment and review boundaries completed on 2026-08-19: `PENDING_PAYMENT` orders expose a payment entry and `COMPLETED` orders expose a review entry from both order lists and the detail page. Both entries validate the loaded order status and display explicit unavailable notices; they do not call an API, charge funds, update order status, or persist reviews. Real payment records, sandbox integration, signed callbacks, idempotency, timeout closure, and the review backend remain deferred.

Latest verification on 2026-08-19: backend `mvn test` passed 77 tests with 0 failures and 0 errors; frontend passed 33 tests and the production build succeeded.

## Phase 8: Farmer onboarding and multi-role authentication

Implemented on 2026-08-19 without replacing Spring Boot or the existing `roles`/`user_roles` model. Flyway V7 adds one-to-one `farmer_profiles` and append-only `farmer_audits`. Public farmer applications create a FARMER account in `PENDING`; pending and rejected farmers cannot log in, approved farmers can log in with their phone, and rejected profiles can be resubmitted without duplicating the user account.

The Vue login page now separates buyer and farmer modes and exposes the administrator channel only as an internal link. Registration separates buyer quick registration from the farmer qualification form. ADMIN users can review applications at `/admin/farmers`. Administrator account creation and fine-grained administrator sub-roles remain deferred; there is still no public administrator registration path.

Latest verification on 2026-08-19: backend passed 80 tests with 0 failures and 0 errors; frontend passed 36 tests and the production build succeeded.

Browser end-to-end acceptance completed on 2026-08-19: a temporary farmer application stayed pending and was blocked from login, an ADMIN approved the farmer, the approved farmer submitted a product, ADMIN approved the product, and a temporary USER saw it in the public catalog and submitted a normal order. The acceptance used local temporary accounts/data only. When an old Spring Boot process is already bound to port 8080, stop it before restarting `mvn spring-boot:run` so the browser uses the current security configuration.
