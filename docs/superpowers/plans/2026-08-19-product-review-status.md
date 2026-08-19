# Product Review Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the P0-1 product review states and V6 schema migration so farmer-created or edited products require review before becoming public.

**Architecture:** Keep the existing modular product domain and MyBatis Plus mapping. Add review state transitions to `Product`, persist nullable audit fields through Flyway V6, and keep public queries unchanged because they already filter strictly on `ON_SALE`.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis Plus, Flyway, MySQL 8, JUnit 5, MockMvc

## Global Constraints

- Do not modify, stage, or commit `.vscode/`.
- Preserve existing `ON_SALE` and `OFF_SALE` rows during migration.
- Farmer create and edit operations must result in `PENDING_REVIEW`.
- Only `ON_SALE` products remain visible and purchasable through public flows.
- P0-2 administrator review HTTP APIs and frontend pages are outside this plan.

---

### Task 1: Define the failing review-state contracts

**Files:**
- Modify: `backend/src/test/java/com/agromall/product/FarmerProductApiTest.java`
- Modify: `backend/src/test/java/com/agromall/product/ProductFlowIntegrationTest.java`
- Modify: `backend/src/test/java/com/agromall/product/ProductSchemaTest.java`

**Interfaces:**
- Consumes: existing farmer product endpoints and MySQL `product` table.
- Produces: failing behavioral tests for `PENDING_REVIEW`, public invisibility, review-field persistence, and resubmission.

- [ ] **Step 1: Change create expectations to pending review**

Update the farmer create assertion to:

```java
.andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
```

- [ ] **Step 2: Change the integration flow to prove pending products are private**

After creation, assert `GET /api/products/{id}` returns 404 with code `1006`, and assert the public list does not contain the created ID.

- [ ] **Step 3: Add a migration persistence assertion**

Use `JdbcTemplate` to update and select `reviewed_by`, `reviewed_at`, and `review_reason`; assert all three values round-trip. Also change the newly created product status expectation to `PENDING_REVIEW`.

- [ ] **Step 4: Run tests and verify RED**

Run:

```powershell
mvn '-Dtest=ProductSchemaTest,FarmerProductApiTest,ProductFlowIntegrationTest' test
```

Expected: failures because creation still returns `ON_SALE` and V6 columns do not exist.

### Task 2: Implement V6 and domain state transitions

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__product_review.sql`
- Modify: `backend/src/main/java/com/agromall/product/domain/ProductStatus.java`
- Modify: `backend/src/main/java/com/agromall/product/domain/Product.java`
- Modify: `backend/src/main/java/com/agromall/product/application/ProductService.java`
- Modify: `backend/src/main/java/com/agromall/product/api/FarmerProductController.java`

**Interfaces:**
- Consumes: `Product.create`, `Product.update`, `ProductService.changeStatus`, and existing farmer endpoints.
- Produces: `Product.submitForReview()`, `Product.approve(Long, LocalDateTime)`, `Product.reject(Long, LocalDateTime, String)`, plus persisted audit getters.

- [ ] **Step 1: Add the migration**

```sql
ALTER TABLE product
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN review_reason VARCHAR(500) NULL,
    ADD CONSTRAINT fk_product_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE INDEX idx_product_reviewed_by ON product(reviewed_by);
```

- [ ] **Step 2: Add enum values and fields**

Add `PENDING_REVIEW` and `REJECTED` to `ProductStatus`, and matching camel-case fields to `Product`.

- [ ] **Step 3: Implement explicit transitions**

`create()` and `update()` call `submitForReview()`. `submitForReview()` sets pending and clears all review fields. `approve()` records reviewer/time, sets `ON_SALE`, and clears the reason. `reject()` requires a nonblank reason, records reviewer/time/reason, and sets `REJECTED`. `offSale()` preserves the prior review audit.

- [ ] **Step 4: Prevent farmer direct publishing**

Change the existing farmer `/on-sale` controller call to request `PENDING_REVIEW`. Update `ProductService.changeStatus` to accept only `OFF_SALE` and `PENDING_REVIEW`, invoking the corresponding domain method; unsupported states fail fast.

- [ ] **Step 5: Run targeted tests and verify GREEN**

Run:

```powershell
mvn '-Dtest=ProductSchemaTest,FarmerProductApiTest,ProductFlowIntegrationTest' test
```

Expected: all targeted tests pass.

### Task 3: Preserve existing sale-dependent test fixtures

**Files:**
- Modify: `backend/src/test/java/com/agromall/product/ProductCatalogApiTest.java`
- Modify: `backend/src/test/java/com/agromall/cart/CartApiTest.java`
- Modify: `backend/src/test/java/com/agromall/order/OrderCheckoutApiTest.java`
- Modify: `backend/src/test/java/com/agromall/order/OrderFlowIntegrationTest.java`
- Modify: `backend/src/test/java/com/agromall/order/OrderSchemaTest.java`
- Modify: `backend/src/test/java/com/agromall/seckill/SeckillSchemaTest.java`
- Modify: `backend/src/test/java/com/agromall/usercenter/UserCenterApiTest.java`

**Interfaces:**
- Consumes: `Product.approve(Long, LocalDateTime)` from Task 2.
- Produces: explicit approved-product fixtures for tests whose subject is catalog, cart, order, seckill, or favorites behavior rather than farmer submission.

- [ ] **Step 1: Approve only fixtures that must be tradable**

Before inserting those products, call:

```java
product.approve(farmer.getId(), LocalDateTime.now());
```

Leave intentionally off-sale fixtures as `product.offSale()` after creation.

- [ ] **Step 2: Run the complete backend suite**

Run:

```powershell
mvn test
```

Expected: all tests pass with zero failures and errors.

### Task 4: Update handoff and publish

**Files:**
- Modify: `docs/handoff/next-work-handoff.md`
- Modify: `docs/handoff/project-handoff.md`

**Interfaces:**
- Consumes: verified implementation and test results.
- Produces: current handoff state identifying P0-1 as complete and P0-2 as the next task.

- [ ] **Step 1: Update both handoff documents**

Record V6, review states, farmer resubmission behavior, verification commands/results, and next work. Remove the outdated claim that product creation directly sets `ON_SALE`.

- [ ] **Step 2: Run repository checks**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; `.vscode/` remains untracked and unstaged.

- [ ] **Step 3: Commit only task files**

Stage explicit backend and documentation paths, inspect `git diff --cached --stat`, and commit with:

```powershell
git commit -m "feat: add product review status migration"
```

- [ ] **Step 4: Push the current branch**

```powershell
git push origin feature/phase-3-cart-orders
```

Expected: the remote branch advances to include the design and implementation commits.
