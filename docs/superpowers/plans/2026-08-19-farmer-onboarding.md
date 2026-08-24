# Farmer Onboarding and Multi-Role Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an independent farmer profile and audit workflow while preserving the existing Spring Boot, JWT, and `user_roles` contracts.

**Architecture:** Extend the auth module with farmer application state and a transactional application service. Keep buyer registration and username login compatible, add farmer-specific endpoints, and reuse the existing admin security boundary and Vue page patterns.

**Tech Stack:** Spring Boot 3.5, MyBatis Plus, Flyway, Spring Security JWT, Vue 3, TypeScript, Vue Router 4, Pinia, Vitest.

## Global Constraints

- Do not migrate to Node.js/Express or replace the existing role junction tables.
- Do not modify or commit `.vscode/`.
- Do not expose an administrator registration path.
- All new production behavior starts with a failing test.
- Existing buyer, product, order, and review tests must remain compatible.

---

### Task 1: Farmer schema and domain contract

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__farmer_onboarding.sql`
- Create: `backend/src/main/java/com/agromall/farmer/domain/FarmerProfile.java`
- Create: `backend/src/main/java/com/agromall/farmer/domain/FarmerApplicationStatus.java`
- Create: `backend/src/main/java/com/agromall/farmer/domain/FarmerAudit.java`
- Create: `backend/src/main/java/com/agromall/farmer/domain/FarmerAuditStatus.java`
- Create: `backend/src/main/java/com/agromall/farmer/infrastructure/FarmerProfileMapper.java`
- Create: `backend/src/main/java/com/agromall/farmer/infrastructure/FarmerAuditMapper.java`
- Test: `backend/src/test/java/com/agromall/farmer/FarmerOnboardingApiTest.java`

- [ ] Write failing persistence/API test for an application creating a farmer profile with `PENDING` status and an audit record.
- [ ] Run the focused test and confirm failure because the endpoint and migration do not exist.
- [ ] Add V7 tables with unique `user_id`, status checks, audit foreign keys, and indexes.
- [ ] Add typed domain objects and MyBatis mappers following existing module patterns.
- [ ] Re-run the focused test after the service endpoint from Task 2 exists.

### Task 2: Farmer application and admin review API

**Files:**
- Create: `backend/src/main/java/com/agromall/farmer/api/FarmerApplicationRequest.java`
- Create: `backend/src/main/java/com/agromall/farmer/api/FarmerProfileView.java`
- Create: `backend/src/main/java/com/agromall/farmer/api/FarmerRejectRequest.java`
- Create: `backend/src/main/java/com/agromall/farmer/api/FarmerController.java`
- Create: `backend/src/main/java/com/agromall/farmer/api/AdminFarmerController.java`
- Create: `backend/src/main/java/com/agromall/farmer/application/FarmerOnboardingService.java`
- Modify: `backend/src/main/java/com/agromall/auth/api/LoginRequest.java`
- Modify: `backend/src/main/java/com/agromall/auth/application/AuthService.java`
- Modify: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`
- Test: `backend/src/test/java/com/agromall/farmer/FarmerOnboardingApiTest.java`

- [ ] Add failing cases for missing farmer fields, duplicate phone, duplicate pending application, USER access to admin review, approve, reject, and resubmit.
- [ ] Implement transactional farmer application: create user, attach FARMER role, insert profile, insert PENDING audit.
- [ ] Implement login lookup by username or phone while preserving old payloads; reject pending/rejected/disabled accounts with stable business errors.
- [ ] Implement ADMIN-only list/detail/approve/reject transitions and reject reason validation.
- [ ] Run farmer integration tests and existing auth/RBAC tests.

### Task 3: Buyer/farmer login and registration UI

**Files:**
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/views/auth/LoginView.vue`
- Modify: `frontend/src/views/auth/RegisterView.vue`
- Modify: `frontend/src/views/auth/RegisterView.spec.ts`
- Create: `frontend/src/views/auth/farmerForm.ts`
- Test: `frontend/src/views/auth/farmerForm.spec.ts`

- [ ] Add failing validation tests for farmer phone, identity number, address, category, password confirmation, and buyer form behavior.
- [ ] Add typed farmer application API while preserving `register()` and `login()` callers.
- [ ] Implement login tabs, internal admin channel copy, buyer registration, farmer application form, agreement checkbox, loading and error states.
- [ ] Keep all copy Simplified Chinese and use existing green/warm palette tokens.
- [ ] Run focused and full frontend tests plus production build.

### Task 4: Admin review page and role navigation

**Files:**
- Create: `frontend/src/api/farmers.ts`
- Create: `frontend/src/api/farmers.spec.ts`
- Create: `frontend/src/views/admin/AdminFarmersView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/styles.css`

- [ ] Add failing API tests for farmer review list, approve, and reject calls.
- [ ] Implement typed API wrappers and admin review page with filters, detail fields, reason input, loading, empty, and error states.
- [ ] Add ADMIN-only route metadata and role-aware navigation without changing existing product/order routes.
- [ ] Run all frontend tests and build.

### Task 5: Handoff, regression, commit, and push

**Files:**
- Modify: `docs/handoff/next-work-handoff.md`
- Modify: `docs/handoff/project-handoff.md`

- [ ] Run `mvn test` in `backend` and record the exact count.
- [ ] Run frontend tests and build and record exact counts.
- [ ] Update handoff docs with schema, endpoints, limitations, and next work.
- [ ] Run `git diff --check`, inspect staged scope, commit, and push the current branch.
