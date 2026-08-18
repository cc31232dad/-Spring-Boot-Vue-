# Phase 4 Redis seckill acceptance

## Automated verification

- `backend`: `mvn -Dtest=SeckillSchemaTest,SeckillRedisReservationTest test` passed.
- `frontend`: `npm run test -- --run` passed, 5 files and 11 tests.
- `frontend`: `npm run build` passed.
- Full backend verification should be run with Docker Redis available: `mvn clean verify`.

## API acceptance

1. Create a product with stock, then create a future seckill activity as an admin.
2. Publish the activity and verify `seckill:stock:{activityId}` is warmed in Redis.
3. Browse `GET /api/seckill` without authentication.
4. Rush with two users and verify each activity stock reservation is atomic.
5. Repeat with the same user and verify the duplicate-purchase error.
6. Force a database insert failure and verify Redis stock and buyer membership are compensated.
7. Cancel a pending seckill order and verify the reservation is restored.
8. Leave a pending order for 15 minutes and verify the scheduled job cancels it and restores Redis state.

## Known environment requirement

The local Docker daemon was unavailable during this verification, so a real Redis concurrency test remains a deployment-environment check.
