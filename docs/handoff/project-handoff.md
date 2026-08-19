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

Implemented: Flyway V6 product review audit columns, `PENDING_REVIEW` and `REJECTED` domain states, farmer create/edit/resubmit transitions, review audit persistence, and public visibility protection. Existing sale-dependent tests now explicitly approve their fixtures. Full backend verification on 2026-08-19: 68 tests passed.
Deferred: administrator review pages, farmer product status page, and end-to-end FARMER -> ADMIN -> USER acceptance flow.

Administrator review API completed on 2026-08-19: `GET /api/admin/products/review`, `POST /api/admin/products/{id}/approve`, and `POST /api/admin/products/{id}/reject`. ADMIN-only access, pending-state guard, reviewer audit fields, reject reason validation, and public visibility are covered by integration tests.

## Recommended next phases

1. Phase 5: Alipay sandbox payment.
2. Phase 6: administrator product review pages and farmer product status page.
3. Phase 7: logistics, reviews, admin statistics, and UI polish.
