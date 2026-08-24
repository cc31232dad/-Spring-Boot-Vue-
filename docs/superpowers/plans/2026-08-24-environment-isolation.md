# Environment and Data Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add explicit `dev`, `test`, and `prod` Spring profiles with separate MySQL, Redis, and upload boundaries without changing business schemas or adding services.

**Architecture:** Keep shared defaults in `application.yml`, place environment-specific values in profile files, and make production secrets mandatory environment variables. Configure Redis key names through one shared prefix property and keep test assertions focused on resolved configuration values.

**Tech Stack:** Spring Boot 3.5, Java 21, YAML configuration, Spring Boot test, MySQL 8, Redis.

## Global Constraints

- Keep the current前后端分离的模块化单体结构; do not split services.
- Do not add business tables, payment code, Docker backend containers, or new dependencies.
- Do not commit credentials, JWT secrets, `.env`, `target`, `dist`, or local database data.
- Preserve local startup without explicit profile by defaulting to `dev`.
- Test profile must use a different database name, Redis prefix, and upload directory from dev.

### Task 1: Add Profile Configuration Files

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/src/main/resources/application-prod.yml`

- [ ] **Step 1: Inspect current property names and write a config contract test**

Add `backend/src/test/java/com/agromall/config/EnvironmentProfileConfigTest.java` using `@ActiveProfiles("test")` and `@SpringBootTest`, asserting the resolved upload directory and Redis prefix are test-specific and the datasource URL contains a test database name.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run `mvn -f backend/pom.xml -Dtest=EnvironmentProfileConfigTest test`.
Expected: FAIL because the test profile and required properties do not yet exist.

- [ ] **Step 3: Add minimal profile YAML**

Keep shared server, Flyway, MyBatis, and multipart settings in `application.yml`; set `spring.profiles.default: dev`. Define `agromall.environment`, `agromall.redis.key-prefix`, and `agromall.upload.product-dir` in each profile. Use `agromall_test` and `agromall_dev` database names, and make `prod` datasource URL, username, password, Redis host/password, JWT secret, and upload directory use `${ENV_VAR}` placeholders with no development fallback.

- [ ] **Step 4: Run the focused test and confirm it passes**

Run `mvn -f backend/pom.xml -Dtest=EnvironmentProfileConfigTest test`.
Expected: PASS.

- [ ] **Step 5: Commit the profile configuration**

Run `git add backend/src/main/resources backend/src/test/java/com/agromall/config/EnvironmentProfileConfigTest.java; git commit -m "feat: isolate spring environments"`.

### Task 2: Apply Redis Prefix at the Existing Boundary

**Files:**
- Modify: existing Redis configuration/service classes found by `rg -n "RedisTemplate|StringRedisTemplate|seckill" backend/src/main/java`
- Test: `backend/src/test/java/com/agromall/config/EnvironmentProfileConfigTest.java`

- [ ] **Step 1: Add a failing assertion for the configured prefix**

Assert that the application exposes one `agromall.redis.key-prefix` value and that the seckill key builder prepends it to inventory and user-limit keys.

- [ ] **Step 2: Run the focused config/seckill tests**

Run `mvn -f backend/pom.xml -Dtest=EnvironmentProfileConfigTest,SeckillRedisReservationTest test`.
Expected: the new prefix assertion fails while existing behavior remains visible.

- [ ] **Step 3: Implement the smallest shared prefix change**

Inject the property into the existing seckill key construction boundary and prepend it to every seckill Redis key, including compensation and timeout paths. Do not duplicate prefixes in callers.

- [ ] **Step 4: Run focused tests**

Run the same Maven command. Expected: PASS with existing Lua reservation behavior unchanged.

- [ ] **Step 5: Commit Redis isolation**

Run `git add backend/src/main/java backend/src/test/java; git commit -m "feat: namespace redis keys by environment"`.

### Task 3: Document Startup and Data Ownership

**Files:**
- Modify: `docs/handoff/current-project-handoff.md`
- Modify: `docs/handoff/future-development-roadmap.md`
- Modify: `README.md`

- [ ] **Step 1: Document commands and required variables**

Add PowerShell examples for `SPRING_PROFILES_ACTIVE=dev|test|prod`, list production variables by name, and state that test uploads and databases are disposable while production data is never seeded from acceptance accounts.

- [ ] **Step 2: Verify documentation references actual properties**

Run `rg -n "SPRING_PROFILES_ACTIVE|AGROMALL_|spring.datasource|key-prefix|upload.product-dir" README.md docs/handoff backend/src/main/resources` and correct mismatches.

- [ ] **Step 3: Commit documentation**

Run `git add README.md docs/handoff/current-project-handoff.md docs/handoff/future-development-roadmap.md; git commit -m "docs: record environment isolation"`.

### Task 4: Full Verification

- [ ] **Step 1:** Run `mvn -f backend/pom.xml test` and confirm all tests pass.
- [ ] **Step 2:** Run `npm --prefix frontend run test -- --run` and confirm all frontend tests pass.
- [ ] **Step 3:** Run `npm --prefix frontend run build` and confirm production build succeeds.
- [ ] **Step 4:** Run `git diff --check` and `git status --short`; confirm only intended files are changed and no generated files are staged.
