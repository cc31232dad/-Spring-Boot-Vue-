# Task 5 Report: JWT 登录与后端授权

## Current status

Implementation was interrupted and must not continue until directed by the controller.

## Files changed/created

Modified:
- `backend/src/main/java/com/agromall/auth/application/AuthService.java`

Created:
- `backend/src/main/java/com/agromall/auth/api/LoginRequest.java`
- `backend/src/main/java/com/agromall/auth/api/SessionController.java`
- `backend/src/main/java/com/agromall/auth/api/TokenView.java`
- `backend/src/main/java/com/agromall/auth/security/CustomUserDetailsService.java`
- `backend/src/main/java/com/agromall/auth/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/agromall/auth/security/JwtService.java`
- `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- `backend/src/test/java/com/agromall/auth/api/AuthorizationApiTest.java`
- `backend/src/test/java/com/agromall/auth/api/LoginApiTest.java`

## RED evidence

Exact command run from `backend`:

```powershell
mvn -gs ..\.superpowers\sdd\maven-settings.xml "-Dmaven.repo.local=C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache" "-Dtest=LoginApiTest,AuthorizationApiTest,RegistrationApiTest" test
```

After the Task 5 tests were created in the correct worktree, Maven exited `1` before executing tests. Output summary:
- `Access is denied.`
- Maven reached `maven-compiler-plugin:3.14.1:testCompile`.
- Maven reported `Fatal error compiling: C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.jar`.

Result: no usable RED for `LoginApiTest,AuthorizationApiTest` was obtained because the sandbox failed during compilation before tests ran.

## Later verification attempt

The same exact Maven command was run again after some production Task 5 code had already been written. Maven exited `1` before executing tests. Output summary:
- `Access is denied.`
- Maven reached `maven-compiler-plugin:3.14.1:compile`.
- Maven reported `Fatal error compiling: C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.jar`.

Result: no GREEN verification was obtained.

## User normal-cmd failure evidence

The controller reported that the user ran Maven in normal cmd and the tests compiled/executed, but all 8 tests errored during Spring `ApplicationContext` startup.

Reported root cause from Surefire:
- `UnsatisfiedDependencyException` creating `authController` -> `authService` -> `jwtService`
- `Failed to instantiate [com.agromall.auth.security.JwtService]: No default constructor found`
- `Caused by: java.lang.NoSuchMethodException: com.agromall.auth.security.JwtService.<init>()`

Assessment: `JwtService` had two constructors (`public JwtService(@Value...)` and package-private `JwtService(String, Duration, Clock)` for testability), and Spring did not select the intended `@Value` constructor unambiguously.

Fix applied:
- Added `@Autowired` to the public `JwtService(@Value("${agromall.jwt.secret}") String secret, @Value("${agromall.jwt.access-token-minutes:30}") long accessTokenMinutes)` constructor.
- Kept the package-private constructor for testability.

Post-fix verification: not yet available. Do not commit until the controller/user provides GREEN verification or this environment can run the Maven command successfully.

## Final GREEN evidence

The user reran verification in normal cmd after the `JwtService` constructor fix. Exact command:

```cmd
cd /d C:\Users\chen\Documents\Codex\2026-07-19\xain\agricultural-mall\.worktrees\phase-1-auth-rbac\backend && mvn -gs ..\.superpowers\sdd\maven-settings.xml "-Dmaven.repo.local=C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache" "-Dtest=LoginApiTest,AuthorizationApiTest,RegistrationApiTest" test
```

Surefire text files in `backend/target/surefire-reports` show:
- `LoginApiTest`: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
- `AuthorizationApiTest`: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
- `RegistrationApiTest`: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

Result: GREEN for all 8 requested tests.

## Blocker assessment

Codex sandbox blocker remains the Maven/JDK ZIP-FS dependency-JAR `AccessDenied` failure described in the task brief. The normal-cmd run exposed a code issue in Spring constructor selection for `JwtService`; the minimal fix has been applied, but it has not yet been verified GREEN.

## Production code status

Yes, production Task 5 code has been written and normal-cmd GREEN evidence has been provided.

## Commit status

Ready to commit once only Task 5 files and this report are staged.
