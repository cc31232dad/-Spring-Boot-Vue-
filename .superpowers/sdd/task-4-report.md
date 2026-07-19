# Task 4 Report: 用户注册
## Blocked before implementation

### TDD evidence

- Baseline: `mvn -gs ..\.superpowers\sdd\maven-settings.xml '-Dmaven.repo.local=..\.m2-repository' test` with Java 21 completed successfully: 7 tests run, 0 failures, 0 errors.
- RED test created first at `backend/src/test/java/com/agromall/auth/api/RegistrationApiTest.java`. A normal user cmd run reached the web layer and failed correctly: expected HTTP 200, actual HTTP 500, caused by `NoResourceFoundException: No static resource api/auth/register.` No registration endpoint existed at that point.
- After that valid RED, the minimal production implementation was added: validated request DTO, session view, transactional service, and controller.
- The duplicate-username test was then added. Both tests keep MockMvc filters disabled only in this Task 4 class, as required while Task 5 security rules do not exist.

### Commands and output summary

1. Required worktree-local cache:
   `mvn -gs ..\.superpowers\sdd\maven-settings.xml '-Dmaven.repo.local=..\.m2-repository' '-Dtest=RegistrationApiTest' test`
   failed during test compilation with `java.nio.file.AccessDeniedException` while Java 21 closed `spring-boot-3.5.16.jar` through ZIP-FS.
2. Authorized workspace cache workaround:
   `mvn -gs ..\.superpowers\sdd\maven-settings.xml '-Dmaven.repo.local=C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache' '-Dtest=RegistrationApiTest' test`
   failed identically on a different dependency, `log4j-to-slf4j-2.24.3.jar`.
3. Minimal normal command-shell launch from the agent:
   `cmd.exe /d /c mvn ... '-Dtest=RegistrationApiTest' test`
   also failed identically on `log4j-to-slf4j-2.24.3.jar`.

### Root cause and concern

The reproducible failure occurs in Java 21's ZIP-FS compiler cleanup (`ZipFileSystemProvider.removeFileSystem` calling `WindowsPath.toRealPath`) for dependency JARs in both caches. Direct JAR listing succeeds and no Java processes remain, so this is an agent-sandbox filesystem restriction rather than a bad dependency JAR or application/test failure. The external normal-shell RED result above supplies valid test-first evidence. The same environment restriction prevents this agent from executing the subsequent GREEN run, so a normal user shell must run the final verification before commit.

### Final GREEN verification

The user ran the following command from a normal Windows cmd session outside the Codex sandbox after both registration tests were present:

`cd /d C:\Users\chen\Documents\Codex\2026-07-19\xain\agricultural-mall\.worktrees\phase-1-auth-rbac\backend && set JAVA_HOME=E:\java21 && set PATH=E:\java21\bin;%PATH% && mvn -gs ..\.superpowers\sdd\maven-settings.xml "-Dmaven.repo.local=C:\Users\chen\Documents\Codex\2026-07-19\xain\maven-cache" "-Dtest=RegistrationApiTest" test`

Result reported by the user: successful. `RegistrationApiTest` passed its two cases: successful registration with the `USER` role, and duplicate username returning HTTP 409 with business code 1001.

### Files changed

- Added `backend/src/test/java/com/agromall/auth/api/RegistrationApiTest.java` with success and duplicate-username cases.
- Added `backend/src/main/java/com/agromall/auth/api/RegisterRequest.java`.
- Added `backend/src/main/java/com/agromall/auth/api/UserSessionView.java`.
- Added `backend/src/main/java/com/agromall/auth/application/AuthService.java`.
- Added `backend/src/main/java/com/agromall/auth/api/AuthController.java`.
- Appended this report. No POM files, cache files, targets, secrets, or environment files were changed.

### Self-review

- Scope remains limited to Task 4's registration endpoint and tests.
- MockMvc filters are disabled only on this test; no `SecurityConfig` or filter chain was added.
- The service checks username and phone uniqueness, BCrypt-hashes the password, inserts the user, binds seeded `USER`, and returns only safe session fields.
- Final GREEN verification was supplied by the user from a normal cmd environment because the Codex sandbox cannot compile changed sources with Java 21 ZIP-FS.
