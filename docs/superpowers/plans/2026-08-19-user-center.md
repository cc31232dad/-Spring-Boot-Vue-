# User Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a database-backed user center covering orders, addresses, favorites, dashboard, and account settings.

**Architecture:** Extend the existing Spring Boot modules with user-center tables, DTOs, mappers, services, and controllers. Extend the Vue Router/Pinia architecture with focused API modules and views under `/user`, while reusing the existing authentication, order, product, and CSS variable conventions.

**Tech Stack:** Spring Boot 3.5, MyBatis, Flyway, MySQL, Vue 3 Composition API, Vue Router 4, Pinia, Axios, Vitest, Vite.

## Global Constraints

- Preserve existing green, warm米白 visual language and existing auth/session behavior.
- Use `ApiResponse<T>` with `code`, `message`, and `data` for every new API.
- Do not add Element Plus or another UI dependency; use existing CSS and native controls.
- Do not stage or modify the user’s untracked `.vscode/` directory.

### Task 1: Database and backend contracts

**Files:** Create `backend/src/main/resources/db/migration/V5__user_center.sql`; create user-center domain/API/infrastructure classes under `backend/src/main/java/com/agromall/usercenter/`; modify order controller/service/mapper for status pagination.

- [ ] Add `user_addresses` and `user_favorites` tables with user ownership, unique constraints, timestamps, and a single-default strategy.
- [ ] Add DTOs and views for address, favorite, profile, password, and phone operations with Bean Validation.
- [ ] Add mapper methods and service methods for CRUD, default address, favorite toggle/list, profile update, password update, phone update.
- [ ] Extend `GET /api/orders/my` to accept optional `status`, `page`, and `size` while retaining existing callers.
- [ ] Add focused backend tests for ownership checks, default-address replacement, favorite idempotency, and order status filtering.
- [ ] Run `mvn test` and commit `feat: add user center backend contracts`.

### Task 2: Frontend API and stores

**Files:** Create `frontend/src/api/user.ts`, `frontend/src/api/addresses.ts`, `frontend/src/api/favorites.ts`, `frontend/src/stores/userCenter.ts`; modify `frontend/src/api/orders.ts` and `frontend/src/stores/orders.ts`.

- [ ] Define typed API functions matching `/api/address`, `/api/favorites`, `/api/user/profile`, `/api/user/password`, and `/api/user/phone`.
- [ ] Add `loadOrders(status, page, size)`, cancel, confirm, and count helpers to the order store.
- [ ] Add address and favorite stores with loading/error state and mutation methods.
- [ ] Add Vitest coverage for status filtering, default-address replacement in store state, and favorite removal.
- [ ] Run `npm run test -- --run` and commit `feat: add user center stores`.

### Task 3: Layout, dropdown, and routing

**Files:** Create `frontend/src/components/user/UserLayout.vue`, `UserDropdown.vue`, `StatusBadge.vue`, `StatCard.vue`, `EmptyState.vue`; create `frontend/src/views/user/UserOverviewView.vue`, `UserOrdersView.vue`, `UserAddressView.vue`, `UserFavoritesView.vue`, `UserSettingsView.vue`; modify `frontend/src/router/index.ts`, `HomeView.vue`, and `styles.css`.

- [ ] Add protected `/user`, `/user/orders`, `/user/address`, `/user/favorites`, `/user/settings` routes and `/user/orders?status=wait|ship|done` query handling.
- [ ] Replace the home header’s plain username with an avatar/name dropdown containing all requested links and logout.
- [ ] Implement the fixed 200px desktop navigation, responsive horizontal mobile navigation, and nested router outlet.
- [ ] Add reusable status, statistic, and empty-state components using existing CSS variables.
- [ ] Run `npm run build` and commit `feat: add user center shell`.

### Task 4: P0 order and address pages

**Files:** Create `frontend/src/components/user/OrderCard.vue`, `AddressCard.vue`, `AddressForm.vue`; complete `UserOrdersView.vue` and `UserAddressView.vue`.

- [ ] Render order tabs and synchronize selected status to the URL query.
- [ ] Render status-specific actions: pay/cancel, logistics/confirm, repurchase/evaluate, and cancelled state.
- [ ] Render address list with masked phone, default badge, edit/delete/set-default actions, and validated add/edit modal form.
- [ ] Add loading, error, success feedback, and confirmation dialogs for destructive actions.
- [ ] Add component tests for tab filtering, address validation, and status action visibility.
- [ ] Run frontend tests and commit `feat: implement user center orders and addresses`.

### Task 5: P1 dashboard and favorites

**Files:** Create `frontend/src/components/user/FavoriteCard.vue`; complete `UserOverviewView.vue` and `UserFavoritesView.vue`; modify `ProductDetailView.vue`.

- [ ] Show four clickable stats and the latest 2–3 orders with status badges.
- [ ] Render responsive 3/4-column favorite grid with remove-on-hover action and empty state.
- [ ] Add favorite toggle to the product detail page and keep it synchronized with the favorite store.
- [ ] Add tests for dashboard counts and favorite toggle behavior.
- [ ] Run frontend tests and commit `feat: add user dashboard and favorites`.

### Task 6: P2 account settings and full verification

**Files:** Complete `UserSettingsView.vue`; modify backend profile/password/phone tests and frontend styles as needed.

- [ ] Implement profile form, masked phone display, password update, and phone update with validation and loading states.
- [ ] Verify all user-center routes at desktop and mobile widths.
- [ ] Run `npm run test -- --run`, `npm run build`, `mvn test`, and `git diff --check`.
- [ ] Commit `feat: complete user center settings` and push `feature/phase-3-cart-orders`.
