# Order Payment and Review Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add truthful payment and review entry points without pretending unsupported backend transactions exist.

**Architecture:** A pure order-action policy maps each `OrderStatus` to supported UI entry points. Order lists route users to the detail page with an action query, and the detail page validates the query against the loaded order before showing a boundary notice.

**Tech Stack:** Vue 3, TypeScript, Vue Router 4, Vitest.

## Global Constraints

- Do not add payment or review APIs, database tables, fake success state, or third-party dependencies.
- All user-facing copy is Simplified Chinese.
- Do not modify or commit `.vscode/`.
- Write and verify a failing test before production code.

---

### Task 1: Order action policy

**Files:**
- Create: `frontend/src/views/orderExperience.ts`
- Test: `frontend/src/views/orderExperience.spec.ts`

**Interfaces:**
- Consumes: `OrderStatus` from `frontend/src/api/orders.ts`.
- Produces: `getOrderExperience(status: OrderStatus): { payment: boolean; review: boolean }`.

- [ ] **Step 1: Write the failing policy tests**

Test that `PENDING_PAYMENT` enables only payment, `COMPLETED` enables only review, and all remaining statuses enable neither.

- [ ] **Step 2: Verify the tests fail**

Run: `npm run test -- --run src/views/orderExperience.spec.ts`

Expected: FAIL because `orderExperience.ts` does not exist.

- [ ] **Step 3: Implement the minimal policy**

Return booleans derived directly from the supplied status.

- [ ] **Step 4: Verify the policy tests pass**

Run: `npm run test -- --run src/views/orderExperience.spec.ts`

Expected: three passing tests.

### Task 2: Wire list and detail entry points

**Files:**
- Modify: `frontend/src/views/OrderDetailView.vue`
- Modify: `frontend/src/views/OrderListView.vue`
- Modify: `frontend/src/views/user/UserOrdersView.vue`
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Consumes: `getOrderExperience(status)` from Task 1.
- Produces: detail links with `action=payment|review` and status-validated notices.

- [ ] **Step 1: Use the policy in both order lists**

Render “去支付” only for `PENDING_PAYMENT` and “评价商品” only for `COMPLETED`; route both to the order detail page with the matching action query.

- [ ] **Step 2: Add detail-page boundary notices**

After loading the order, accept `action=payment` only for `PENDING_PAYMENT` and `action=review` only for `COMPLETED`. Add matching detail buttons and explanatory `role=status` text. Neither handler calls an API.

- [ ] **Step 3: Add focused responsive styling**

Style the inline notice and action links using the existing green and neutral palette, preserving the current mobile footer layout.

- [ ] **Step 4: Run focused and full frontend verification**

Run: `npm run test -- --run src/views/orderExperience.spec.ts`

Run: `npm run test -- --run`

Run: `npm run build`

Expected: all commands exit 0.

### Task 3: Handoff and repository verification

**Files:**
- Modify: `docs/handoff/next-work-handoff.md`
- Modify: `docs/handoff/project-handoff.md`

**Interfaces:**
- Consumes: fresh verification counts and implemented behavior.
- Produces: current project status and next recommended work.

- [ ] **Step 1: Run backend regression tests**

Run: `mvn test` in `backend`.

Expected: all tests pass with 0 failures and 0 errors.

- [ ] **Step 2: Update both handoff documents**

Record the truthful payment boundary, review entry point, exact fresh test counts, and that real payment/review backends remain P2 work.

- [ ] **Step 3: Check repository scope**

Run: `git diff --check`, `git status --short`, and `git ls-files .vscode`.

Expected: no whitespace errors and no tracked `.vscode` files.

- [ ] **Step 4: Commit and push**

Stage only the files named in this plan, commit with `feat: clarify order payment and review boundaries`, and push `feature/phase-3-cart-orders` to its configured remote.
