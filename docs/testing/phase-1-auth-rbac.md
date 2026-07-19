# Phase 1 authentication and RBAC acceptance

Test date: 2026-07-19

## Automated commands

```powershell
cd backend
mvn clean verify

cd frontend
npm run test -- --run
npm run build
```

The backend command was attempted in the Codex sandbox with the worktree Maven settings. It was blocked during test compilation by an `AccessDenied` error while reading `spring-boot-3.5.16.jar`; see the task report for the exact evidence. The frontend commands must be run from a normal command prompt if the sandbox denies access to esbuild.

Normal Windows command prompt verification completed on 2026-07-19:

- Backend `mvn clean verify`: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Frontend `npm run test -- --run`: `2 passed`.
- Frontend `npm run build`: production build completed successfully.

## Acceptance coverage

`AuthFlowIntegrationTest` performs the following in one transaction:

- Registers a new USER account and confirms its `USER` role.
- Logs in and obtains a Bearer access token.
- Uses that token to call `GET /api/auth/me` successfully.
- Uses the same USER token to call `GET /api/farmer/test` and expects `403 Forbidden`.
- Reads the persisted user and confirms the password value is not plaintext (and is BCrypt-formatted).

Existing authorization tests additionally cover the unauthenticated `GET /api/auth/me` result: `401 Unauthorized` with application code `1003`.

## Validation results

| Validation | Expected result | Evidence / status |
| --- | --- | --- |
| Registration | Successful account registration returns USER session data | Covered by `AuthFlowIntegrationTest`; normal-command `mvn clean verify` passed. |
| Login | Valid credentials return a Bearer token | Covered by `AuthFlowIntegrationTest`; normal-command `mvn clean verify` passed. |
| 401 | Anonymous `/api/auth/me` returns 401 and code 1003 | Covered by `AuthorizationApiTest`; normal-command `mvn clean verify` passed. |
| 403 | USER `/api/farmer/test` returns 403 and code 1004 | Covered by `AuthFlowIntegrationTest`; normal-command `mvn clean verify` passed. |

No real access token or password is recorded in this document.

## Manual browser acceptance checklist

1. Register an account and log in to reach the home page.
2. Refresh the page and confirm the session is restored.
3. Log out and confirm the home page redirects to login.
4. In browser developer tools, confirm authenticated requests send only a Bearer token and do not send a password.
5. Confirm Git does not track `.env`, database data directories, or keys.
