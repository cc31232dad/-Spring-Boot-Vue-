# Sandbox Payment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an internal HMAC-based sandbox payment flow that creates payment records, verifies callbacks idempotently, closes expired payments, and advances paid orders to shipment.

**Architecture:** Add a `payment` module inside the existing Spring Boot modular monolith. `PaymentService` owns payment state transitions and delegates conditional order transitions to `OrderMapper`; the sandbox controller is the only unauthenticated callback boundary. A scheduled job closes expired records using the same service path as manual tests.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis Plus, Flyway, MySQL 8, Vue 3, Pinia/Axios, Vitest.

## Global Constraints

- No third-party payment SDK, real payment credentials, bank data, or callback body storage.
- All money comparisons use `BigDecimal`; all state transitions use conditional database updates.
- Preserve `ApiResponse<T>`, existing JWT ownership checks, and current modular-monolith boundaries.
- Sandbox secret is injected through `AGROMALL_SANDBOX_PAYMENT_SECRET`; production has no fallback.
- Use TDD: every new behavior gets a failing test before implementation.

### Task 1: Add Payment Schema and Configuration

**Files:**
- Create: `backend/src/main/resources/db/migration/V8__payment_records.sql`
- Modify: `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`, `.env.example`
- Test: `backend/src/test/java/com/agromall/payment/PaymentSchemaTest.java`

- [ ] **Step 1:** Write schema assertions for `payment_records`, unique `payment_no`, indexed `order_id`, amount/status columns and allowed lifecycle values.
- [ ] **Step 2:** Run `mvn -f backend/pom.xml -Dtest=PaymentSchemaTest test`; expect failure because V8/table is absent.
- [ ] **Step 3:** Add the migration with `PENDING`, `PAID`, `FAILED`, `CLOSED`, decimal amount, timestamps, callback count and indexes. Add `agromall.payment.sandbox-secret` to dev/test via env override and prod as `${AGROMALL_SANDBOX_PAYMENT_SECRET}`.
- [ ] **Step 4:** Run the focused schema test; expect pass.
- [ ] **Step 5:** Commit `feat: add sandbox payment schema`.

### Task 2: Implement Payment Domain, Mapper, and HMAC Utility

**Files:**
- Create: `backend/src/main/java/com/agromall/payment/domain/PaymentRecord.java`
- Create: `backend/src/main/java/com/agromall/payment/domain/PaymentStatus.java`
- Create: `backend/src/main/java/com/agromall/payment/api/PaymentView.java`
- Create: `backend/src/main/java/com/agromall/payment/api/SandboxPaymentCallbackRequest.java`
- Create: `backend/src/main/java/com/agromall/payment/infrastructure/PaymentMapper.java`
- Create: `backend/src/main/java/com/agromall/payment/application/PaymentSignature.java`
- Test: `backend/src/test/java/com/agromall/payment/PaymentSignatureTest.java`

- [ ] **Step 1:** Write tests for deterministic canonical payload, valid HMAC-SHA256 signature, invalid signature rejection, and timestamp window rejection.
- [ ] **Step 2:** Run the focused test; expect failure because utility types do not exist.
- [ ] **Step 3:** Implement immutable canonical field order `paymentNo|orderNo|amount|result|timestamp`, constant-time signature comparison, hex signature encoding, and a configurable callback window.
- [ ] **Step 4:** Add mapper queries for pending-by-order, pending-expired records, conditional paid/failed/closed updates, and callback metadata updates.
- [ ] **Step 5:** Run focused signature and compilation tests; expect pass.
- [ ] **Step 6:** Commit `feat: add sandbox payment primitives`.

### Task 3: Implement Payment Service and APIs

**Files:**
- Create: `backend/src/main/java/com/agromall/payment/application/PaymentService.java`
- Create: `backend/src/main/java/com/agromall/payment/api/PaymentController.java`
- Create: `backend/src/main/java/com/agromall/payment/api/SandboxPaymentController.java`
- Modify: `backend/src/main/java/com/agromall/order/infrastructure/OrderMapper.java`
- Modify: `backend/src/main/java/com/agromall/order/application/OrderService.java` only if shared order lookup/status helper is required
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Tests: `backend/src/test/java/com/agromall/payment/PaymentApiTest.java`, `PaymentServiceTest.java`

- [ ] **Step 1:** Write failing tests for buyer ownership, pending-only creation, idempotent pending record reuse, signature/amount/order mismatch rejection, successful callback, failed callback, duplicate callback, and conditional order transition.
- [ ] **Step 2:** Run focused payment tests; expect failures for missing endpoints and service.
- [ ] **Step 3:** Implement `POST /api/orders/{id}/payment` returning/reusing a 15-minute sandbox payment record and `POST /api/payments/sandbox/callback` as a permit-all signed boundary.
- [ ] **Step 4:** On success, conditionally update payment to `PAID` and order `PENDING_PAYMENT -> PENDING_SHIPMENT`; on failure, conditionally update payment to `FAILED` only. Return the existing terminal result for duplicate callbacks.
- [ ] **Step 5:** Add `ErrorCode` entries and global responses for payment not found, invalid status, signature, amount, and expired callback cases.
- [ ] **Step 6:** Run focused API/service tests; expect pass.
- [ ] **Step 7:** Commit `feat: implement sandbox payment APIs`.

### Task 4: Add Expiry Job and Inventory-Safe Closure

**Files:**
- Create: `backend/src/main/java/com/agromall/payment/application/PaymentExpiryJob.java`
- Modify: `backend/src/main/java/com/agromall/payment/application/PaymentService.java`
- Modify: `backend/src/main/java/com/agromall/order/infrastructure/OrderMapper.java`
- Tests: `backend/src/test/java/com/agromall/payment/PaymentExpiryTest.java`

- [ ] **Step 1:** Write tests for closing only expired pending records, cancelling still-pending orders, restoring normal-order stock once, and delegating seckill release without double compensation.
- [ ] **Step 2:** Run focused expiry tests; expect failure.
- [ ] **Step 3:** Implement a fixed-delay scheduled scan and a service method that conditionally closes payment first, then changes the order only if still pending. Reuse existing order item/product restoration and seckill cancellation boundaries.
- [ ] **Step 4:** Run focused expiry tests and existing seckill/order tests; expect pass.
- [ ] **Step 5:** Commit `feat: close expired sandbox payments`.

### Task 5: Add Frontend Sandbox Payment Experience

**Files:**
- Modify: `frontend/src/api/orders.ts` or create `frontend/src/api/payment.ts`
- Modify: `frontend/src/views/OrderDetailView.vue`
- Modify: `frontend/src/views/OrderListView.vue`, `frontend/src/views/user/UserOrdersView.vue` only where the existing payment link is shared
- Tests: `frontend/src/api/payment.spec.ts`, `frontend/src/views/orderPayment.spec.ts`

- [ ] **Step 1:** Write failing tests for payment API payload, payment panel visibility only for pending orders, rendering payment number/amount/expiry, success/failure actions and refresh after success.
- [ ] **Step 2:** Run focused Vitest tests; expect failure.
- [ ] **Step 3:** Add typed create-payment and sandbox-callback API functions using the configured secret only through backend-generated signatures; render a clearly labelled sandbox panel with success/failure controls and loading/error states.
- [ ] **Step 4:** Keep URL action validation from `orderExperience`; after success reload the order and show the new `PENDING_SHIPMENT` state.
- [ ] **Step 5:** Run focused Vitest tests and production build; expect pass.
- [ ] **Step 6:** Commit `feat: add sandbox payment frontend flow`.

### Task 6: Full Verification and Handoff

- [ ] **Step 1:** Run `mvn -f backend/pom.xml test`.
- [ ] **Step 2:** Run `npm --prefix frontend run test -- --run`.
- [ ] **Step 3:** Run `npm --prefix frontend run build`.
- [ ] **Step 4:** Perform browser acceptance: create pending order, open payment, simulate success, verify order becomes待发货; simulate failure and verify it remains待支付; repeat success callback and verify no duplicate transition.
- [ ] **Step 5:** Update `docs/handoff/future-development-roadmap.md` and `docs/handoff/current-project-handoff.md` with payment endpoints, secret configuration, test results and remaining real-channel limitations.
- [ ] **Step 6:** Run `git diff --check`, inspect `git status --short`, and commit `docs: record sandbox payment verification`.
