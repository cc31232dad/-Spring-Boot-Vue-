# Phase 2 product catalog acceptance

Test date: 2026-07-19

## Automated commands

```powershell
cd backend
mvn clean verify

cd frontend
npm run test -- --run
npm run build
```

`ProductFlowIntegrationTest` covers a farmer registering and logging in, publishing a product, finding it in the public catalog, opening its public detail, taking it off sale, and receiving product-not-found from the public detail API afterward.

## Manual checklist

1. Open the home page and confirm categories load.
2. Search for a seeded or newly created product.
3. Filter by category.
4. Open product detail.
5. Log in as farmer/admin and create a product.
6. Confirm ordinary user cannot open product publishing API.
7. Take a product off sale and confirm public listing hides it.
