# 商品图片上传 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让农户通过桌面拖拽/点击或移动端相册选择商品图片，系统完成安全上传并自动保存现有 `imageUrl` 字段。

**Architecture:** 在现有 Spring Boot 单体中新增认证上传接口和本地文件存储服务，通过 Spring 静态资源映射暴露图片；Vue 商品表单先上传图片取得 URL，再使用现有商品创建/更新接口。数据模型不变，上传存储通过配置抽象，后续可替换为 MinIO/OSS。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring MVC multipart、Spring Security、JUnit/MockMvc、Vue 3、TypeScript、Vitest、Docker Compose。

## Global Constraints

- 只支持 JPG、JPEG、PNG、WEBP，单文件最大 5 MB。
- 同时校验文件内容、MIME 类型和扩展名；禁止使用用户原始文件名。
- 移动端只支持相册选择，暂不加入拍照上传。
- 不修改 `.vscode/`，不把图片二进制存入数据库。
- 商品仍通过 `imageUrl` 字段保存图片地址。
- 所有生产代码遵循 TDD：先写失败测试，再实现最小行为。

---

### Task 1: 后端上传领域与配置

**Files:**
- Create: `backend/src/main/java/com/agromall/product/application/ProductImageStorage.java`
- Create: `backend/src/main/java/com/agromall/product/application/LocalProductImageStorage.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductImageController.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductImageView.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/agromall/product/ProductImageUploadApiTest.java`

**Interfaces:**
- `ProductImageStorage.store(MultipartFile file): String` returns a URL such as `/uploads/products/<random>.webp`.
- `ProductImageController.upload(principal, file)` handles `POST /api/farmer/product-images` and returns `ApiResponse<ProductImageView>`.
- Only authenticated approved farmers may upload; unauthenticated users receive the existing authentication response.

- [ ] **Step 1: Write failing API tests**

Add MockMvc cases for: valid JPEG returns 200 and `/uploads/products/` URL; empty file returns 400; oversized file returns 400; unsupported MIME/content returns 400; buyer/admin without farmer role is rejected; uploaded bytes are written under the configured directory.

- [ ] **Step 2: Run the focused test and verify failure**

Run `mvn -f backend/pom.xml -Dtest=ProductImageUploadApiTest test`. Expected: compilation failure because the upload controller and storage types do not exist.

- [ ] **Step 3: Implement storage and endpoint**

Use `agromall.upload.product-dir` with default `uploads/products`. Generate a UUID-based filename, normalize the extension from detected content, create directories, copy bytes, and return `/uploads/products/<filename>`. Reject files over 5 MB, empty files, unsupported media types, and files whose magic bytes do not match the declared image type. Add a farmer-only route rule and keep the existing JWT principal contract.

- [ ] **Step 4: Run focused tests and verify green**

Run the same Maven command. Expected: all upload API cases pass with no failures.

- [ ] **Step 5: Commit**

`git add backend/src/main backend/src/test && git commit -m "feat: add farmer product image upload"`

### Task 2: Static resource serving and container persistence

**Files:**
- Create: `backend/src/main/java/com/agromall/product/config/ProductImageResourceConfig.java`
- Modify: `docker-compose.yml`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/agromall/product/ProductImageResourceConfigTest.java`

**Interfaces:**
- `GET /uploads/products/<filename>` serves only files inside the configured product upload directory.
- Docker Compose mounts `./backend/uploads/products` to the backend upload directory when the backend service is introduced; for the current compose file, document the required host directory and volume contract without adding an unconfigured backend container.

- [ ] **Step 1: Write failing resource-serving test**

Verify a stored fixture is readable through the `/uploads/products/` resource handler and a path traversal filename is not served.

- [ ] **Step 2: Run test and verify failure**

Run `mvn -f backend/pom.xml -Dtest=ProductImageResourceConfigTest test`. Expected: failure because no resource handler exists.

- [ ] **Step 3: Implement resource mapping and documented volume**

Register a `ResourceHandler` rooted at the normalized upload directory, reject traversal through the resource resolver, add `AGROMALL_UPLOAD_PRODUCT_DIR` configuration support, and add the host-directory/volume instructions to the roadmap or handoff documentation.

- [ ] **Step 4: Run backend upload and resource tests**

Run `mvn -f backend/pom.xml -Dtest='ProductImage*Test' test`. Expected: all pass.

- [ ] **Step 5: Commit**

`git add backend/src/main backend/src/test docker-compose.yml docs && git commit -m "feat: serve uploaded product images"`

### Task 3: Frontend upload API and farmer form

**Files:**
- Modify: `frontend/src/api/products.ts`
- Modify: `frontend/src/views/farmer/ProductFormView.vue`
- Modify: `frontend/src/styles.css` only if shared upload styles are needed
- Test: `frontend/src/api/products.spec.ts`
- Test: `frontend/src/views/farmer/ProductFormView.spec.ts`

**Interfaces:**
- `uploadProductImage(file: File): Promise<string>` posts `FormData` field `file` to `/farmer/product-images` and returns the URL.
- The form tracks `selectedImage`, `previewUrl`, `isUploading`, and `dragActive`; `submitProduct` cannot run while an image is uploading or when a new file failed to upload.

- [ ] **Step 1: Write failing frontend tests**

Test that the API sends multipart `file`, the form renders the upload control, selecting a valid image previews it and calls the API, invalid type/size shows an error, drag/drop follows the same path, replacing/removing updates the preview, and submit is disabled during upload.

- [ ] **Step 2: Run focused Vitest tests and verify failure**

Run `npm --prefix frontend run test -- --run src/api/products.spec.ts src/views/farmer/ProductFormView.spec.ts`. Expected: missing upload function/spec behavior failures.

- [ ] **Step 3: Implement upload interaction**

Use a hidden file input with `accept="image/jpeg,image/png,image/webp"`, a visible drop zone, object URLs for previews, client-side 5 MB/type checks, and an explicit retry state. On edit, existing `imageUrl` remains the preview until replacement. Do not add `capture` to the input, so camera upload remains deferred.

- [ ] **Step 4: Run focused and full frontend tests**

Run focused tests, then `npm --prefix frontend run test -- --run` and `npm --prefix frontend run build`. Expected: all pass and production build succeeds.

- [ ] **Step 5: Commit**

`git add frontend/src && git commit -m "feat: add farmer image picker and drag drop"`

### Task 4: End-to-end verification and handoff update

**Files:**
- Modify: `docs/handoff/current-project-handoff.md`
- Modify: `docs/handoff/future-development-roadmap.md`
- Test: existing backend and frontend suites

- [ ] **Step 1: Run complete verification**

Run `mvn -f backend/pom.xml test`, `npm --prefix frontend run test -- --run`, `npm --prefix frontend run build`, `git diff --check`, and `git status --short`. Expected: backend/frontend tests pass, build passes, no whitespace errors, and no `.vscode` changes.

- [ ] **Step 2: Perform browser acceptance**

With MySQL, Redis, backend, and frontend running, log in as the approved farmer, create a product using a local image, verify preview and submission, then verify the image in farmer list, admin review, and buyer product detail. Repeat selection from a mobile-sized viewport using the gallery file picker path. Confirm no console errors and no horizontal overflow.

- [ ] **Step 3: Update handoff**

Record the upload endpoint, configuration key, storage directory, volume requirement, tests, and the deferred camera-upload decision. Move phase 1 to completed only after browser acceptance succeeds.

- [ ] **Step 4: Commit**

`git add docs && git commit -m "docs: record product image upload verification"`
