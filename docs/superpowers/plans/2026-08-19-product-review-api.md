# Product Review API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add secure administrator APIs for listing and reviewing pending products.

**Architecture:** Extend the existing product application service with administrator review use cases, add small API DTOs and controller under the product module, and reuse `Product.approve/reject` for state transitions and audit fields.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, MyBatis Plus, JUnit 5, MockMvc

## Global Constraints

- Keep `/api/admin/**` protected by the existing `ADMIN` role rule.
- Only `PENDING_REVIEW` products may be approved or rejected.
- Reject reasons are required, trimmed, and limited to 500 characters.
- Do not modify or commit `.vscode/`.
- Do not add frontend work in this P0-2 backend task.

---

### Task 1: Add failing admin review API tests

**Files:**
- Create: `backend/src/test/java/com/agromall/product/AdminProductReviewApiTest.java`
- Modify: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`

**Interfaces:**
- Consumes: existing JWT login, `ProductMapper`, product review schema, and `/api/admin/products/review` contracts.
- Produces: failing tests for listing, approve, reject, validation, illegal status, and role protection.

- [ ] **Step 1: Write tests**

Cover these exact behaviors:

```java
GET /api/admin/products/review -> 200 and pending product fields
POST /api/admin/products/{id}/approve -> 200, status ON_SALE, public GET -> 200
POST /api/admin/products/{id}/reject with {"reason":"图片不清晰"} -> 200, status REJECTED, reason persisted
POST reject with {"reason":"  "} -> 400 code 1005
POST approve twice -> 409 code 1023
USER/FARMER admin requests -> 403 code 1004
```

- [ ] **Step 2: Run RED**

Run `mvn '-Dtest=AdminProductReviewApiTest' test` from `backend`. Expected failures must show missing controller/endpoint or missing error code, not compilation mistakes in the test.

### Task 2: Implement review application and API layer

**Files:**
- Modify: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/agromall/common/exception/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/agromall/product/application/ProductService.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductReviewView.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductRejectRequest.java`
- Create: `backend/src/main/java/com/agromall/product/api/AdminProductReviewController.java`

**Interfaces:**
- Consumes: tests from Task 1 and existing `Product.approve/reject` methods.
- Produces: `ProductService.listReviewProducts(ProductStatus)`, `approveProduct(Long, Long)`, `rejectProduct(Long, Long, String)` and the three HTTP endpoints.

- [ ] **Step 1: Add error code and conflict mapping**

Add `PRODUCT_REVIEW_INVALID(1023, "Product review state is invalid")` and include it in the conflict branch of `GlobalExceptionHandler`.

- [ ] **Step 2: Add validated request and response DTOs**

```java
public record ProductRejectRequest(@NotBlank @Size(max = 500) String reason) {}
public record ProductReviewView(Long id, Long categoryId, String categoryName, Long farmerId,
                                String name, String description, BigDecimal price, Integer stock,
                                String originPlace, String imageUrl, String status,
                                Long reviewedBy, LocalDateTime reviewedAt, String reviewReason) {}
```

- [ ] **Step 3: Add service methods**

List by status with category names; approve/reject load the product, require `PENDING_REVIEW`, call the domain transition, update the row, and return `ProductReviewView`.

- [ ] **Step 4: Add controller endpoints**

Use `@AuthenticationPrincipal JwtService.JwtPrincipal`, `@RequestParam(defaultValue = "PENDING_REVIEW") ProductStatus status`, `@PostMapping("/{id}/approve")`, and `@PostMapping("/{id}/reject")` under `/api/admin/products`.

- [ ] **Step 5: Run targeted GREEN tests**

Run `mvn '-Dtest=AdminProductReviewApiTest' test`; expected result is all tests passing.

### Task 3: Full verification and handoff

**Files:**
- Modify: `docs/handoff/next-work-handoff.md`
- Modify: `docs/handoff/project-handoff.md`

- [ ] **Step 1: Run backend and frontend verification**

Run `mvn test` in `backend`, then `npm run test -- --run` and `npm run build` in `frontend`.

- [ ] **Step 2: Update handoff**

Mark P0-2 backend API complete, document exact endpoints and test counts, and set the next task to the admin review page and farmer product status page.

- [ ] **Step 3: Check, commit, and push**

Run `git diff --check`, stage explicit paths excluding `.vscode/`, commit `feat: add admin product review api`, and push `feature/phase-3-cart-orders`.
