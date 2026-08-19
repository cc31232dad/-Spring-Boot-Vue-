# Three-Surface Backoffice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate the application into a buyer mall, farmer workbench, and administrator backoffice without duplicating the Vue application or adding backend APIs.

**Architecture:** Introduce nested `FarmerLayout` and `AdminLayout` route trees around existing role pages. Route post-login destinations from the authenticated user's actual roles. Compose current farmer/admin APIs into small typed dashboard loaders using `Promise.allSettled`, then render compact role-specific work surfaces.

**Tech Stack:** Vue 3, TypeScript, Composition API, Vue Router 4, Pinia, Axios, Vitest, Lucide Vue icons, Spring Boot 3.5 existing APIs.

## Global Constraints

- Keep one Vue application and one Spring Boot modular monolith.
- Preserve `USER`, `FARMER`, `ADMIN` and existing `roles` / `user_roles` authorization.
- `/farmer/**` requires `FARMER`; `/admin/**` requires `ADMIN`; backend ownership and Spring Security remain final boundaries.
- Do not add operator, customer-service, super-administrator, administrator-creation, database, or backend API work.
- Use only real data from existing farmer/admin product and order APIs.
- Use Simplified Chinese for all visible copy.
- Do not modify or commit `.vscode/`, `.env`, `dist`, `target`, or `node_modules`.

---

### Task 1: Role-aware login and nested role routes

**Files:**
- Create: `frontend/src/views/auth/loginNavigation.ts`
- Test: `frontend/src/views/auth/loginNavigation.spec.ts`
- Create: `frontend/src/views/farmer/FarmerLayout.vue`
- Create: `frontend/src/views/farmer/FarmerDashboardView.vue`
- Create: `frontend/src/views/admin/AdminLayout.vue`
- Create: `frontend/src/views/admin/AdminDashboardView.vue`
- Modify: `frontend/src/views/auth/LoginView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/HomeView.vue`

**Interfaces:**
- Produces `resolvePostLoginRoute(roles: string[], redirect?: string): string`.
- Produces nested route roots `/farmer` and `/admin` with inherited role metadata.

- [ ] Write `loginNavigation.spec.ts` asserting ADMIN -> `/admin`, FARMER -> `/farmer`, USER -> `/`, valid local redirects are retained, and `https://` / `//` redirects are rejected.
- [ ] Run `cd frontend; npm test -- --run src/views/auth/loginNavigation.spec.ts` and verify failure because the helper is missing.
- [ ] Implement:

```ts
export function resolvePostLoginRoute(roles: string[], redirect?: string) {
  const fallback = roles.includes('ADMIN') ? '/admin' : roles.includes('FARMER') ? '/farmer' : '/'
  return redirect?.startsWith('/') && !redirect.startsWith('//') ? redirect : fallback
}
```

- [ ] Update `LoginView.submit()` to log in, call `auth.loadCurrentUser()`, and navigate using the helper and the existing redirect query.
- [ ] Replace standalone farmer/admin records with these nested roots:

```ts
{
  path: '/farmer',
  component: FarmerLayout,
  meta: { requiresAuth: true, requiredRole: 'FARMER' },
  children: [
    { path: '', name: 'farmer-dashboard', component: FarmerDashboardView },
    { path: 'products', name: 'farmer-products', component: FarmerProductsView },
    { path: 'products/new', name: 'farmer-product-new', component: ProductFormView },
    { path: 'products/:id/edit', name: 'farmer-product-edit', component: ProductFormView },
    { path: 'orders', name: 'farmer-orders', component: FarmerOrdersView }
  ]
}
```

```ts
{
  path: '/admin',
  component: AdminLayout,
  meta: { requiresAuth: true, requiredRole: 'ADMIN' },
  children: [
    { path: '', name: 'admin-dashboard', component: AdminDashboardView },
    { path: 'products', name: 'admin-products', component: AdminProductsView },
    { path: 'farmers', name: 'admin-farmers', component: AdminFarmersView },
    { path: 'orders', name: 'admin-orders', component: AdminOrdersView }
  ]
}
```

- [ ] Add minimal `<RouterView />` shells and dashboard placeholders so the route tree compiles.
- [ ] In `HomeView.vue`, show buyer controls only for `USER`, `进入农户工作台` for `FARMER`, and `进入管理后台` for `ADMIN`; keep public catalog access for all roles.
- [ ] Run login navigation and auth Store tests; commit `feat: separate buyer farmer and admin routes`.

---

### Task 2: Typed farmer and administrator dashboard loaders

**Files:**
- Create: `frontend/src/views/farmer/farmerDashboard.ts`
- Test: `frontend/src/views/farmer/farmerDashboard.spec.ts`
- Create: `frontend/src/views/admin/adminDashboard.ts`
- Test: `frontend/src/views/admin/adminDashboard.spec.ts`
- Modify: `frontend/src/views/farmer/FarmerDashboardView.vue`
- Modify: `frontend/src/views/admin/AdminDashboardView.vue`

**Interfaces:**
- `loadFarmerDashboard(): Promise<FarmerDashboardData>` consumes `listFarmerProducts()` and `listFarmerOrders()`.
- `loadAdminDashboard(): Promise<AdminDashboardData>` consumes two `listAdminProducts()` calls, `listFarmerApplications()`, and `listAdminOrders()`.

- [ ] Write failing farmer tests mocking both APIs. Assert product status counts, pending-shipment count, five most recent orders, and partial success when either request rejects.
- [ ] Write failing admin tests mocking all APIs. Assert pending product, on-sale product, pending farmer, order counts, five most recent orders, and partial success.
- [ ] Run both test files and verify failure because loaders are missing.
- [ ] Implement settled-result helpers that return `null` for failed counts and typed error keys. Farmer output:

```ts
interface FarmerDashboardData {
  products: { total: number | null; pending: number | null; onSale: number | null; rejected: number | null }
  orders: { total: number | null; pendingShipment: number | null }
  recentOrders: Order[]
  errors: Array<'products' | 'orders'>
}
```

- [ ] Implement admin output:

```ts
interface AdminDashboardData {
  stats: { pendingProducts: number | null; onSaleProducts: number | null; pendingFarmers: number | null; orders: number | null }
  recentOrders: Order[]
  errors: Array<'pendingProducts' | 'onSaleProducts' | 'pendingFarmers' | 'orders'>
}
```

- [ ] Render farmer metrics, pending work links, recent orders, partial warnings, loading, empty, and reload states.
- [ ] Render admin metrics, review work links, recent orders, partial warnings, loading, empty, and reload states.
- [ ] Run focused tests; commit `feat: add farmer and administrator dashboards`.

---

### Task 3: Independent role layouts and compact management pages

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/src/views/farmer/FarmerLayout.vue`
- Modify: `frontend/src/views/admin/AdminLayout.vue`
- Modify: `frontend/src/views/farmer/FarmerProductsView.vue`
- Modify: `frontend/src/views/farmer/ProductFormView.vue`
- Modify: `frontend/src/views/farmer/FarmerOrdersView.vue`
- Modify: `frontend/src/views/admin/AdminProductsView.vue`
- Modify: `frontend/src/views/admin/AdminFarmersView.vue`
- Modify: `frontend/src/views/admin/AdminOrdersView.vue`
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Layouts consume route names and `useAuthStore`; pages render only role content within `<RouterView />`.

- [ ] Install only `lucide-vue-next` using `cd frontend; npm install lucide-vue-next`.
- [ ] Build `FarmerLayout` with icons for 工作台、商品管理、订单处理、查看商城、退出登录 and a mobile menu button. Use leaf green `#2f7d4a` as the sidebar identity.
- [ ] Build `AdminLayout` with icons for 工作台、商品审核、农户审核、订单管理、查看商城、退出登录 and a mobile menu button. Use archive green `#173f2b` as the sidebar identity.
- [ ] Both layouts show the actual account name, current route title, active navigation state, close mobile navigation after route changes, and route to `/login` after logout.
- [ ] Remove duplicate mall return headers and outer page shells from all farmer/admin child pages. Preserve all existing API actions, ownership, confirmation, loading, error, and empty states.
- [ ] Translate every visible English string in `FarmerOrdersView.vue` and `AdminOrdersView.vue` to Simplified Chinese.
- [ ] Add scoped `.farmer-shell` and `.admin-shell` styles: stable 240px desktop sidebar, 64px top bar, mist work area, maximum 8px panel radius, compact metrics and lists, no gradients, visible focus, responsive 760px overlay navigation, stacked mobile actions, and reduced-motion support.
- [ ] Run `cd frontend; npm run build`; commit `feat: build farmer and administrator workspaces`.

---

### Task 4: Route regression, browser acceptance, handoff, and delivery

**Files:**
- Create: `frontend/src/router/roleWorkspaces.spec.ts`
- Modify: `docs/handoff/next-work-handoff.md`
- Modify: `docs/handoff/project-handoff.md`

**Interfaces:**
- Verifies finalized role route contracts and documents exact completion boundaries.

- [ ] Write a raw-source route test asserting one nested `/farmer` root with `FARMER`, one nested `/admin` root with `ADMIN`, and all named child routes.
- [ ] Run `cd frontend; npm test -- --run` and `npm run build`.
- [ ] Run `cd backend; mvn -Dtest=AuthorizationApiTest,AdminProductReviewApiTest,FarmerOnboardingApiTest,OrderPermissionApiTest test`.
- [ ] Start or reuse current backend and frontend. In Browser, verify at desktop 1440x900 and mobile 390x844:
  - `acceptadmin / AdminPass819!` lands on `/admin`;
  - admin child navigation never renders mall chrome;
  - an approved farmer account lands on `/farmer` and sees only own products/orders;
  - public mall shows the correct workspace return link for each role;
  - a USER cannot render either workspace;
  - no overlap, clipped labels, blank regions, broken control states, or console errors.
- [ ] Update both handoff documents, explicitly deferring administrator creation, role subdivisions, user management, payments, refunds, real logistics, reviews, and new analytics data.
- [ ] Run `git diff --check`, confirm no `.vscode`, run the full backend `mvn test`, then commit `test: verify separate role workspaces`.
- [ ] Push `feature/phase-3-cart-orders` and verify local/remote HEAD equality and a clean worktree.
