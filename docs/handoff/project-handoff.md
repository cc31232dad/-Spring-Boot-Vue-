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

Completed: cart, checkout, stock deduction, farmer-scoped orders, shopper/farmer/admin order pages.
Deferred: payment, seckill, logistics, refunds, coupons, reviews.
Problems encountered: the initial acceptance test used a regular expression that could not safely extract an order ID from a multi-order checkout response; it was replaced with JSON tree traversal. The full backend suite currently has four failures in the pre-existing `OrderCheckoutApiTest` for the same order-ID-versus-order-item-ID regex issue when database auto-increment values diverge. Maven also reports Mockito dynamic-agent warnings on JDK 21.
Verification: see `docs/testing/phase-3-cart-orders.md`.

## Recommended next phases

1. Phase 4: seckill with Redis stock pre-deduction.
2. Phase 5: Alipay sandbox payment.
3. Phase 6: logistics, reviews, admin statistics, and UI polish.
