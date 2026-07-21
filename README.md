# Agricultural Mall

Phase 1 provides account registration, login, JWT session recovery, and role-based API access. Phase 2 adds the product catalog, including public browsing and farmer/admin product management. Phase 3 adds cart management and normal orders, including farmer-split checkout and order status workflows.

## Prerequisites

- JDK 21
- Node.js 22
- Docker (with Docker Compose)

## Local startup

Copy the example environment file and replace every placeholder with local development values. Do not commit the resulting `.env` file.

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Start the backend in one terminal:

```powershell
cd backend
mvn spring-boot:run
```

Start the frontend in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Default local ports are:

- Frontend (Vite): `5173`
- Backend: `8080`
- MySQL: `3306`
- Redis: `6379`

## Tests

Run the backend suite:

```powershell
cd backend
mvn clean verify
```

Run the frontend tests and production build:

```powershell
cd frontend
npm run test -- --run
npm run build
```

For the Phase 2 product catalog acceptance flow and browser checklist, see [docs/testing/phase-2-product-catalog.md](docs/testing/phase-2-product-catalog.md).

For the Phase 3 cart and normal orders acceptance flow and browser checklist, see [docs/testing/phase-3-cart-orders.md](docs/testing/phase-3-cart-orders.md).

## Security notes

- `.env` is local-only and must never be committed.
- No default administrator password is provided. Local development scripts must explicitly create administrator accounts when they are needed.
- Client requests authenticate with a Bearer token; never send a password except to the login or registration endpoint.
