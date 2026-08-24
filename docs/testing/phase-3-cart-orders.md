# Phase 3 cart and normal orders acceptance

Latest verified: 2026-07-21

## Automated commands

```powershell
cd backend
mvn clean verify

cd ..\frontend
npm run test -- --run
npm run build
```

`OrderFlowIntegrationTest` covers shopper registration and login, two farmer-owned products in one cart, cross-farmer checkout into two orders, stock deduction, farmer shipment, shopper completion, and cancellation with stock restoration.

Latest command evidence:

- `cd backend && mvn -Dtest=OrderCheckoutApiTest test`: 10 tests, 0 failures, 0 errors.
- `cd backend && mvn -Dtest=OrderFlowIntegrationTest test`: 1 test, 0 failures, 0 errors.
- `cd backend && mvn clean verify`: 58 tests, 0 failures, 0 errors.
- `cd frontend && npm run test -- --run`: 4 files, 9 tests passed.
- `cd frontend && npm run build`: production build completed.

## Manual checklist

1. Login as a normal user.
2. Open product detail and add a product to cart.
3. Open cart and update quantity.
4. Checkout with receiver information.
5. Confirm stock is reduced.
6. Confirm cart item is removed after checkout.
7. Open my orders and see the new order.
8. Login as farmer and see the assigned order.
9. Farmer marks order as shipped.
10. User confirms receipt and order becomes completed.
11. Try ordering more than stock and confirm it fails cleanly.
