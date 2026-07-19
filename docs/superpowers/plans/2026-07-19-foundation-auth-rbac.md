# Foundation, Authentication and RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可运行的 Spring Boot 3 + Vue 3 助农商城骨架，实现用户注册、JWT 登录与 `USER`、`FARMER`、`ADMIN` 三类角色授权。

**Architecture:** 单仓库包含 `backend` 与 `frontend`。后端按业务领域分包，MySQL 作为权威数据源，Flyway 管理结构，Spring Security 负责鉴权；前端使用 Pinia 保存会话，通过 Axios 拦截器携带令牌。第一阶段不实现农户业务，只预置角色并验证授权边界。

**Tech Stack:** JDK 21、Spring Boot 3.5.16、Spring Security、MyBatis-Plus 3.5.16、Flyway、MySQL 8.4、JJWT 0.13.0、Vue 3、Vite 8.1、TypeScript、Pinia、Vue Router、Axios、Vitest。

## Global Constraints

- 后端必须保持模块化单体，不加入 Spring Cloud、注册中心、网关或消息队列。
- Java 金额统一使用 `BigDecimal`，数据库金额统一使用 `DECIMAL`。
- 密码必须使用 BCrypt；JWT 密钥从环境变量 `AGROMALL_JWT_SECRET` 读取。
- 数据库密码从环境变量读取，密钥与密码不得提交 Git。
- API 统一响应字段为 `code`、`message`、`data`。
- 包根路径固定为 `com.agromall`；前端页面文案使用“助农商城”。
- 每个任务严格执行红灯、绿灯、提交的 TDD 循环。

---

## File Structure

```text
backend/
  pom.xml                                  Maven 依赖与构建
  src/main/java/com/agromall/
    AgriculturalMallApplication.java      后端入口
    common/api/ApiResponse.java            统一响应
    common/exception/                       错误码与全局异常
    auth/api/                               注册登录接口与 DTO
    auth/application/AuthService.java       注册登录用例
    auth/security/                           JWT 与 Spring Security
    user/domain/                             用户与角色实体
    user/infrastructure/                     MyBatis-Plus Mapper
  src/main/resources/
    application.yml                         公共配置
    db/migration/V1__auth_schema.sql         用户权限表
  src/test/java/com/agromall/                后端测试
frontend/
  package.json                               前端脚本与依赖
  src/api/                                   Axios 客户端与认证接口
  src/stores/auth.ts                         会话状态
  src/router/index.ts                        路由守卫
  src/views/auth/                            登录与注册页
  src/views/HomeView.vue                     登录后首页
docker-compose.yml                           本地 MySQL 与 Redis
.env.example                                 环境变量模板
```

### Task 1: 可启动的后端与基础设施

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/agromall/AgriculturalMallApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/agromall/AgriculturalMallApplicationTest.java`
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `.gitignore`

**Interfaces:**
- Consumes: 无。
- Produces: 可启动的 Spring Context；MySQL `localhost:3306/agromall`；Redis `localhost:6379`。

- [ ] **Step 1: 写上下文失败测试**

```java
package com.agromall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AgriculturalMallApplicationTest {
    @Test void contextLoads() {}
}
```

- [ ] **Step 2: 验证测试失败**

Run: `cd backend && mvn -q test`

Expected: FAIL，原因是 `pom.xml` 和应用入口尚不存在。

- [ ] **Step 3: 创建最小后端工程**

`pom.xml` 使用 Spring Boot `3.5.16`、Java `21`，加入 `web`、`validation`、`security`、`data-redis`、MyBatis-Plus `3.5.16`、Flyway、MySQL、JJWT `0.13.0`、Lombok 和 test starter。JJWT 的 `jjwt-api` 使用 compile scope，`jjwt-impl` 与 `jjwt-jackson` 使用 runtime scope。

```java
package com.agromall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgriculturalMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriculturalMallApplication.class, args);
    }
}
```

```yaml
spring:
  application.name: agricultural-mall
  datasource:
    url: ${AGROMALL_DB_URL:jdbc:mysql://localhost:3306/agromall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${AGROMALL_DB_USERNAME:agromall}
    password: ${AGROMALL_DB_PASSWORD:agromall_dev}
  data.redis:
    host: ${AGROMALL_REDIS_HOST:localhost}
    port: ${AGROMALL_REDIS_PORT:6379}
  flyway.enabled: true
server.port: ${SERVER_PORT:8080}
agromall.jwt:
  secret: ${AGROMALL_JWT_SECRET}
  access-token-minutes: 30
```

`docker-compose.yml` 创建 `mysql:8.4` 与 `redis:7.4-alpine`，MySQL 数据库名、用户与密码分别为 `agromall`、`agromall`、`agromall_dev`，并配置健康检查。

- [ ] **Step 4: 运行基础验证**

Run: `$env:AGROMALL_JWT_SECRET='dev-only-secret-at-least-32-bytes'; docker compose up -d; cd backend; mvn test`

Expected: `BUILD SUCCESS`，两个容器状态为 healthy。

- [ ] **Step 5: 提交**

```bash
git add .gitignore .env.example docker-compose.yml backend
git commit -m "chore: bootstrap Spring Boot backend"
```

### Task 2: 统一响应与异常边界

**Files:**
- Create: `backend/src/main/java/com/agromall/common/api/ApiResponse.java`
- Create: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/agromall/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/agromall/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/agromall/common/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `ApiResponse<T>(int code, String message, T data)`；`BusinessException(ErrorCode)`。

- [ ] **Step 1: 写异常映射测试**

```java
@WebMvcTest
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mvc;
    @Test void mapsBusinessExceptionToConflict() throws Exception {
        mvc.perform(get("/test-error"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value(1001));
    }
    @TestConfiguration static class Config {
        @RestController static class C {
            @GetMapping("/test-error") void fail() {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
            }
        }
    }
}
```

- [ ] **Step 2: 运行并确认红灯**

Run: `cd backend && mvn -Dtest=GlobalExceptionHandlerTest test`

Expected: FAIL，相关类型不存在。

- [ ] **Step 3: 实现稳定错误结构**

```java
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "success", data); }
    public static ApiResponse<Void> error(ErrorCode e) { return new ApiResponse<>(e.code(), e.message(), null); }
}
```

`ErrorCode` 至少定义 `USER_ALREADY_EXISTS(1001)`、`INVALID_CREDENTIALS(1002)`、`UNAUTHORIZED(1003)`、`FORBIDDEN(1004)`、`VALIDATION_ERROR(1005)`、`INTERNAL_ERROR(9999)`。`GlobalExceptionHandler` 将业务冲突映射为 409、校验错误映射为 400，未知异常记录日志后返回 500，响应中不包含调用栈。

- [ ] **Step 4: 运行测试**

Run: `cd backend && mvn -Dtest=GlobalExceptionHandlerTest test`

Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/agromall/common backend/src/test/java/com/agromall/common
git commit -m "feat: add API response and exception boundary"
```

### Task 3: 用户角色数据模型

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__auth_schema.sql`
- Create: `backend/src/main/java/com/agromall/user/domain/User.java`
- Create: `backend/src/main/java/com/agromall/user/domain/Role.java`
- Create: `backend/src/main/java/com/agromall/user/infrastructure/UserMapper.java`
- Create: `backend/src/main/java/com/agromall/user/infrastructure/RoleMapper.java`
- Create: `backend/src/main/java/com/agromall/user/infrastructure/UserRoleMapper.java`
- Create: `backend/src/test/java/com/agromall/user/infrastructure/UserMapperTest.java`

**Interfaces:**
- Produces: `UserMapper.selectByUsername(String)`；`RoleMapper.selectCodesByUserId(Long)`；数据库角色 `USER`、`FARMER`、`ADMIN`。

- [ ] **Step 1: 写 Mapper 集成测试**

```java
@SpringBootTest
@Transactional
class UserMapperTest {
    @Autowired UserMapper users;
    @Test void findsUserByUsername() {
        User user = User.create("alice", "hash", "13800000000");
        users.insert(user);
        assertThat(users.selectByUsername("alice")).isPresent();
    }
}
```

- [ ] **Step 2: 运行并确认红灯**

Run: `cd backend && mvn -Dtest=UserMapperTest test`

Expected: FAIL，迁移与 Mapper 尚不存在。

- [ ] **Step 3: 创建结构与实体**

迁移创建 `users`、`roles`、`user_roles`。`users.username`、`users.phone` 唯一；`user_roles(user_id, role_id)` 使用联合唯一索引；插入三个固定角色。所有表使用 `BIGINT` 主键、`created_at`、`updated_at`，用户包含 `enabled` 与 `version`。

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    default Optional<User> selectByUsername(String username) {
        return Optional.ofNullable(selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
    }
}
```

`RoleMapper.selectCodesByUserId(Long)` 使用显式 SQL 联结 `roles` 与 `user_roles` 返回 `Set<String>`。

- [ ] **Step 4: 运行测试**

Run: `cd backend && mvn -Dtest=UserMapperTest test`

Expected: PASS，Flyway 显示 schema version `1`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/resources/db backend/src/main/java/com/agromall/user backend/src/test/java/com/agromall/user
git commit -m "feat: add user and role persistence"
```

### Task 4: 用户注册

**Files:**
- Create: `backend/src/main/java/com/agromall/auth/api/RegisterRequest.java`
- Create: `backend/src/main/java/com/agromall/auth/api/UserSessionView.java`
- Create: `backend/src/main/java/com/agromall/auth/application/AuthService.java`
- Create: `backend/src/main/java/com/agromall/auth/api/AuthController.java`
- Create: `backend/src/test/java/com/agromall/auth/api/RegistrationApiTest.java`

**Interfaces:**
- Consumes: `UserMapper`、`RoleMapper`、`UserRoleMapper`。
- Produces: `POST /api/auth/register`；`AuthService.register(RegisterRequest)`。

- [ ] **Step 1: 写注册接口测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationApiTest {
    @Autowired MockMvc mvc;
    @Test void registersUserWithUserRole() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
          .content("""{"username":"alice","password":"Passw0rd!","phone":"13800000000"}"""))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.username").value("alice"))
          .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }
}
```

- [ ] **Step 2: 运行并确认红灯**

Run: `cd backend && mvn -Dtest=RegistrationApiTest test`

Expected: FAIL 404。

- [ ] **Step 3: 实现注册事务**

`RegisterRequest` 使用 Bean Validation：用户名 4–32 位字母数字下划线，密码 8–64 位且至少包含字母与数字，手机号为 11 位数字。`AuthService.register` 检查用户名与手机号唯一性，BCrypt 加密密码，在同一事务插入用户并绑定 `USER` 角色。Controller 仅接收 DTO 并返回 `ApiResponse<UserSessionView>`。

- [ ] **Step 4: 增加重复用户名测试并运行**

Run: `cd backend && mvn -Dtest=RegistrationApiTest test`

Expected: PASS；重复注册返回 HTTP 409、业务码 `1001`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/agromall/auth backend/src/test/java/com/agromall/auth
git commit -m "feat: add user registration"
```

### Task 5: JWT 登录与后端授权

**Files:**
- Create: `backend/src/main/java/com/agromall/auth/api/LoginRequest.java`
- Create: `backend/src/main/java/com/agromall/auth/api/TokenView.java`
- Create: `backend/src/main/java/com/agromall/auth/security/JwtService.java`
- Create: `backend/src/main/java/com/agromall/auth/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/agromall/auth/security/CustomUserDetailsService.java`
- Create: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/agromall/auth/api/SessionController.java`
- Create: `backend/src/test/java/com/agromall/auth/api/LoginApiTest.java`
- Create: `backend/src/test/java/com/agromall/auth/api/AuthorizationApiTest.java`

**Interfaces:**
- Produces: `POST /api/auth/login`；`GET /api/auth/me`；`JwtService.issue(User, Set<String>)`；Bearer token 鉴权。

- [ ] **Step 1: 写登录与权限测试**

```java
@Test void loginReturnsBearerToken() throws Exception {
    mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
      .content("""{"username":"alice","password":"Passw0rd!"}"""))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
}
@Test void anonymousCannotReadSession() throws Exception {
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
}
```

- [ ] **Step 2: 运行并确认红灯**

Run: `cd backend && mvn -Dtest=LoginApiTest,AuthorizationApiTest test`

Expected: FAIL，登录接口不存在且安全链未配置。

- [ ] **Step 3: 实现 JWT 与无状态安全链**

JWT `sub` 保存用户 ID，`username` 与 `roles` 保存为 claims，过期时间为 30 分钟，使用至少 256 位 HMAC 密钥。过滤器只接受 `Authorization: Bearer <token>`，解析失败不写认证对象。安全规则：`/api/auth/register`、`/api/auth/login` 允许匿名；`/api/admin/**` 需要 `ADMIN`；`/api/farmer/**` 需要 `FARMER`；其他 `/api/**` 需要登录；关闭 session 与 CSRF，配置 JSON 形式的 401/403。

- [ ] **Step 4: 验证登录、错误密码与角色隔离**

Run: `cd backend && mvn -Dtest=LoginApiTest,AuthorizationApiTest test`

Expected: PASS；错误密码返回 401/`1002`；USER 访问 farmer/admin 分别返回 403/`1004`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/agromall/auth backend/src/test/java/com/agromall/auth
git commit -m "feat: add JWT authentication and RBAC"
```

### Task 6: Vue 应用骨架与登录体验

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/RegisterView.vue`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/stores/auth.spec.ts`

**Interfaces:**
- Consumes: `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`。
- Produces: `/login`、`/register`、`/`；Pinia `useAuthStore()`。

- [ ] **Step 1: 写会话 Store 失败测试**

```ts
it('stores a successful login token', async () => {
  vi.spyOn(authApi, 'login').mockResolvedValue({ accessToken: 'jwt', tokenType: 'Bearer' })
  const store = useAuthStore()
  await store.login({ username: 'alice', password: 'Passw0rd!' })
  expect(store.accessToken).toBe('jwt')
})
```

- [ ] **Step 2: 运行并确认红灯**

Run: `cd frontend && npm install && npm run test -- --run`

Expected: FAIL，Store 尚不存在。

- [ ] **Step 3: 实现前端最小闭环**

`http.ts` 创建 `baseURL: '/api'` 的 Axios 实例，请求拦截器从 Store 获取 token，响应遇到 401 时清理会话并跳转 `/login`。`auth.ts` 提供类型化 `register`、`login`、`me`。Store 只持久化访问令牌，不持久化密码或用户敏感资料。路由守卫在访问 `/` 时要求登录。

登录页包含用户名、密码、提交与错误提示；注册页包含用户名、手机号、密码和确认密码；首页展示“助农商城”及当前用户名，并提供退出登录。样式使用独立 CSS 变量，不复制参考项目视觉资源。

- [ ] **Step 4: 运行测试和构建**

Run: `cd frontend && npm run test -- --run && npm run build`

Expected: 所有 Vitest 测试 PASS，Vite build 成功且无 TypeScript 错误。

- [ ] **Step 5: 提交**

```bash
git add frontend
git commit -m "feat: add Vue authentication experience"
```

### Task 7: 第一阶段端到端验收与文档

**Files:**
- Create: `README.md`
- Create: `docs/testing/phase-1-auth-rbac.md`
- Create: `backend/src/test/java/com/agromall/auth/AuthFlowIntegrationTest.java`

**Interfaces:**
- Consumes: 第一阶段全部接口。
- Produces: 可复现的本地启动流程和阶段测试证据。

- [ ] **Step 1: 写完整认证流程测试**

测试依次执行注册、登录、携带 token 读取 `/api/auth/me`、USER 访问 `/api/farmer/test` 得到 403，并断言数据库密码不是明文。

- [ ] **Step 2: 运行后端、前端全部测试**

Run: `cd backend && mvn clean verify`

Expected: `BUILD SUCCESS`。

Run: `cd frontend && npm run test -- --run && npm run build`

Expected: 测试与构建全部成功。

- [ ] **Step 3: 编写运行说明**

README 必须包含 JDK 21、Node.js 22、Docker 的前置要求，环境变量复制方式、`docker compose up -d`、`mvn spring-boot:run`、`npm run dev`、测试命令、默认端口，以及“不提供默认管理员密码；由开发脚本在本地显式创建”的安全说明。

`phase-1-auth-rbac.md` 记录测试日期、命令、通过项，以及注册、登录、401、403 四类验证结果，不记录真实 token 或密码。

- [ ] **Step 4: 人工验收**

1. 注册 `alice`；
2. 登录并进入首页；
3. 刷新页面后会话仍可恢复；
4. 退出后访问首页跳转登录；
5. 用浏览器开发工具确认请求只发送 Bearer token，不发送密码；
6. 确认 Git 中不存在 `.env`、数据库数据目录或任何密钥。

- [ ] **Step 5: 提交阶段成果**

```bash
git add README.md docs/testing backend/src/test
git commit -m "test: verify authentication and RBAC phase"
```

## Phase Completion Gate

进入第二阶段前必须同时满足：后端 `mvn clean verify` 成功、前端测试与构建成功、注册登录可人工演示、USER 无法访问 FARMER/ADMIN 接口、Git 密钥扫描无结果、README 可让新环境独立启动项目。
