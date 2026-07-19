# Product Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Phase 2 product catalog capabilities: fixed agricultural categories, public product browsing, farmer/admin product management, and Vue product pages.

**Architecture:** Keep the modular monolith structure. Add a focused `product` backend package with `api`, `application`, `domain`, and `infrastructure` subpackages, backed by Flyway + MyBatis-Plus. Add frontend `products` API/store and small route-level views for list, detail, and farmer form.

**Tech Stack:** JDK 21, Spring Boot 3.5.16, Spring Security, MyBatis-Plus 3.5.16, Flyway, MySQL 8.4, Vue 3, Vite, TypeScript, Pinia, Vue Router, Axios, Vitest.

## Global Constraints

- Keep the backend as a modular monolith; do not add Spring Cloud, registry, gateway, message queue, or distributed transaction infrastructure.
- Java monetary values use `BigDecimal`; database monetary values use `DECIMAL`.
- Product stock is displayed and managed in Phase 2; stock deduction belongs to future order and seckill phases.
- Product images are represented by `imageUrl`; do not implement binary upload in Phase 2.
- Public visitors can browse only `ON_SALE` products.
- `FARMER` can manage only their own products; `ADMIN` can manage all products.
- `USER` cannot create, edit, or change product sale status.
- API responses keep the existing `ApiResponse { code, message, data }` envelope.
- Every task follows red-green-commit TDD.

---

## File Structure

```text
backend/src/main/java/com/agromall/product/
  api/
    CategoryController.java                Public category API
    ProductController.java                 Public product list/detail API
    FarmerProductController.java           Farmer/admin management API
    ProductCreateRequest.java              Product create validation DTO
    ProductUpdateRequest.java              Product update validation DTO
    CategoryView.java                      Category response DTO
    ProductSummaryView.java                Product card response DTO
    ProductDetailView.java                 Product detail response DTO
  application/
    ProductService.java                    Product use cases and permission checks
  domain/
    Product.java                           MyBatis-Plus product entity
    ProductCategory.java                   MyBatis-Plus category entity
    ProductStatus.java                     ON_SALE/OFF_SALE enum
  infrastructure/
    ProductMapper.java                     Product persistence
    ProductCategoryMapper.java             Category persistence
backend/src/main/resources/db/migration/
  V2__product_catalog.sql                  Product schema and fixed categories
backend/src/test/java/com/agromall/product/
  ProductCatalogApiTest.java               Public category/list/detail tests
  FarmerProductApiTest.java                Product management authorization tests

frontend/src/api/
  products.ts                              Product/category API functions and types
frontend/src/stores/
  products.ts                              Product list/detail state
  products.spec.ts                         Product store tests
frontend/src/views/
  HomeView.vue                             Mall home with search/filter/cards
  ProductDetailView.vue                    Product detail page
  farmer/ProductFormView.vue               Farmer/admin create/edit form
frontend/src/router/index.ts               Product and farmer routes
```

### Task 1: Product Schema and Seeded Categories

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__product_catalog.sql`
- Create: `backend/src/main/java/com/agromall/product/domain/ProductStatus.java`
- Create: `backend/src/main/java/com/agromall/product/domain/ProductCategory.java`
- Create: `backend/src/main/java/com/agromall/product/domain/Product.java`
- Create: `backend/src/main/java/com/agromall/product/infrastructure/ProductCategoryMapper.java`
- Create: `backend/src/main/java/com/agromall/product/infrastructure/ProductMapper.java`
- Create: `backend/src/test/java/com/agromall/product/ProductSchemaTest.java`

**Interfaces:**
- Consumes: existing `users.id` table from `V1__auth_schema.sql`.
- Produces:
  - Table `product_category(id, name, sort_order, created_at)`
  - Table `product(id, category_id, farmer_id, name, description, price, stock, origin_place, image_url, status, created_at, updated_at)`
  - Mapper method `ProductCategoryMapper.selectList(null)`
  - Mapper method `ProductMapper.insert(Product product)`

- [ ] **Step 1: Write the failing schema test**

Create `backend/src/test/java/com/agromall/product/ProductSchemaTest.java`:

```java
package com.agromall.product;

import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductCategoryMapper;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductSchemaTest {

    @Autowired ProductCategoryMapper categoryMapper;
    @Autowired ProductMapper productMapper;
    @Autowired UserMapper userMapper;

    @Test
    void seedsCategoriesAndPersistsProduct() {
        assertThat(categoryMapper.selectList(null))
                .extracting("name")
                .containsExactly("水果", "蔬菜", "粮油", "禽蛋肉类", "茶叶特产");

        User farmer = User.create("schemafarmer", "hash", "13900000001");
        userMapper.insert(farmer);

        Product product = Product.create(
                1L,
                farmer.getId(),
                "洛川苹果",
                "脆甜红富士苹果",
                new BigDecimal("29.90"),
                100,
                "陕西洛川",
                "https://example.com/apple.jpg"
        );

        productMapper.insert(product);

        Product saved = productMapper.selectById(product.getId());
        assertThat(saved.getStatus()).isEqualTo("ON_SALE");
        assertThat(saved.getPrice()).isEqualByComparingTo("29.90");
        assertThat(saved.getStock()).isEqualTo(100);
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=ProductSchemaTest test
```

Expected: FAIL because `Product`, mappers, and V2 migration do not exist.

- [ ] **Step 3: Add migration**

Create `backend/src/main/resources/db/migration/V2__product_catalog.sql`:

```sql
CREATE TABLE product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO product_category (id, name, sort_order) VALUES
    (1, '水果', 1),
    (2, '蔬菜', 2),
    (3, '粮油', 3),
    (4, '禽蛋肉类', 4),
    (5, '茶叶特产', 5);

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    origin_place VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id),
    CONSTRAINT fk_product_farmer FOREIGN KEY (farmer_id) REFERENCES users(id),
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT chk_product_stock CHECK (stock >= 0)
);

CREATE INDEX idx_product_status_created ON product(status, created_at);
CREATE INDEX idx_product_category_status ON product(category_id, status);
CREATE INDEX idx_product_farmer ON product(farmer_id);
```

- [ ] **Step 4: Add product domain and mapper classes**

Create `ProductStatus.java`:

```java
package com.agromall.product.domain;

public enum ProductStatus {
    ON_SALE,
    OFF_SALE
}
```

Create `ProductCategory.java`:

```java
package com.agromall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("product_category")
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

Create `Product.java`:

```java
package com.agromall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long farmerId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String originPlace;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Product create(Long categoryId, Long farmerId, String name, String description,
                                 BigDecimal price, Integer stock, String originPlace, String imageUrl) {
        Product product = new Product();
        product.categoryId = categoryId;
        product.farmerId = farmerId;
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        product.originPlace = originPlace;
        product.imageUrl = imageUrl;
        product.status = ProductStatus.ON_SALE.name();
        return product;
    }

    public void update(Long categoryId, String name, String description, BigDecimal price,
                       Integer stock, String originPlace, String imageUrl) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.originPlace = originPlace;
        this.imageUrl = imageUrl;
    }

    public void onSale() {
        this.status = ProductStatus.ON_SALE.name();
    }

    public void offSale() {
        this.status = ProductStatus.OFF_SALE.name();
    }
}
```

Create mappers:

```java
package com.agromall.product.infrastructure;

import com.agromall.product.domain.ProductCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
```

```java
package com.agromall.product.infrastructure;

import com.agromall.product.domain.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
```

- [ ] **Step 5: Run test and commit**

Run:

```powershell
cd backend
mvn -Dtest=ProductSchemaTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/resources/db/migration/V2__product_catalog.sql backend/src/main/java/com/agromall/product backend/src/test/java/com/agromall/product/ProductSchemaTest.java
git commit -m "feat: add product catalog schema"
```

### Task 2: Public Category and Product Query APIs

**Files:**
- Modify: `backend/src/main/java/com/agromall/common/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/agromall/common/exception/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/agromall/auth/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/agromall/product/api/CategoryController.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductController.java`
- Create: `backend/src/main/java/com/agromall/product/api/CategoryView.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductSummaryView.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductDetailView.java`
- Create: `backend/src/main/java/com/agromall/product/application/ProductService.java`
- Create: `backend/src/test/java/com/agromall/product/ProductCatalogApiTest.java`

**Interfaces:**
- Consumes: `ProductMapper`, `ProductCategoryMapper`, `ProductStatus.ON_SALE`.
- Produces:
  - `GET /api/categories`
  - `GET /api/products?keyword=&categoryId=`
  - `GET /api/products/{id}`
  - `ProductService.listCategories()`
  - `ProductService.listPublicProducts(String keyword, Long categoryId)`
  - `ProductService.getPublicProduct(Long id)`

- [ ] **Step 1: Write failing API test**

Create `ProductCatalogApiTest.java` with tests for category list, public filters, detail, and off-sale hiding. Use test data inserted through mappers. The expected failure is missing controllers/service.

Key assertions:

```java
mvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("水果"));

mvc.perform(get("/api/products").param("keyword", "苹果").param("categoryId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("洛川苹果"));

mvc.perform(get("/api/products/{id}", offSaleProductId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(1006));
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=ProductCatalogApiTest test
```

Expected: FAIL because routes do not exist.

- [ ] **Step 3: Extend error codes and status mapping**

Add to `ErrorCode` before `INTERNAL_ERROR`:

```java
PRODUCT_NOT_FOUND(1006, "Product not found"),
CATEGORY_NOT_FOUND(1007, "Category not found"),
```

Update `GlobalExceptionHandler.statusFor`:

```java
case PRODUCT_NOT_FOUND, CATEGORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
```

- [ ] **Step 4: Permit public category/product routes**

Update `SecurityConfig`:

```java
.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
.requestMatchers(HttpMethod.GET, "/api/categories", "/api/products", "/api/products/*").permitAll()
```

Add import:

```java
import org.springframework.http.HttpMethod;
```

- [ ] **Step 5: Implement views, controllers, and service**

Create compact Java records:

```java
public record CategoryView(Long id, String name) {}
public record ProductSummaryView(Long id, Long categoryId, String categoryName, String name,
                                 BigDecimal price, Integer stock, String originPlace, String imageUrl) {}
public record ProductDetailView(Long id, Long categoryId, String categoryName, Long farmerId,
                                String name, String description, BigDecimal price, Integer stock,
                                String originPlace, String imageUrl, String status) {}
```

`CategoryController`:

```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final ProductService productService;
    public CategoryController(ProductService productService) { this.productService = productService; }
    @GetMapping
    public ApiResponse<List<CategoryView>> list() {
        return ApiResponse.ok(productService.listCategories());
    }
}
```

`ProductController`:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }
    @GetMapping
    public ApiResponse<List<ProductSummaryView>> list(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(productService.listPublicProducts(keyword, categoryId));
    }
    @GetMapping("/{id}")
    public ApiResponse<ProductDetailView> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getPublicProduct(id));
    }
}
```

`ProductService` should use `LambdaQueryWrapper<Product>`:

```java
wrapper.eq(Product::getStatus, ProductStatus.ON_SALE.name());
if (StringUtils.hasText(keyword)) wrapper.like(Product::getName, keyword.trim());
if (categoryId != null) wrapper.eq(Product::getCategoryId, categoryId);
wrapper.orderByDesc(Product::getCreatedAt);
```

Build a `Map<Long, String>` from categories to attach `categoryName`.

- [ ] **Step 6: Run tests and commit**

Run:

```powershell
cd backend
mvn -Dtest=ProductCatalogApiTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/java/com/agromall/common/exception backend/src/main/java/com/agromall/auth/security/SecurityConfig.java backend/src/main/java/com/agromall/product backend/src/test/java/com/agromall/product/ProductCatalogApiTest.java
git commit -m "feat: add public product catalog APIs"
```

### Task 3: Farmer/Admin Product Management APIs

**Files:**
- Create: `backend/src/main/java/com/agromall/product/api/FarmerProductController.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductCreateRequest.java`
- Create: `backend/src/main/java/com/agromall/product/api/ProductUpdateRequest.java`
- Modify: `backend/src/main/java/com/agromall/product/application/ProductService.java`
- Create: `backend/src/test/java/com/agromall/product/FarmerProductApiTest.java`

**Interfaces:**
- Consumes: Phase 2 Task 2 `ProductService`, Phase 1 JWT authentication principal.
- Produces:
  - `POST /api/farmer/products`
  - `PUT /api/farmer/products/{id}`
  - `PATCH /api/farmer/products/{id}/off-sale`
  - `PATCH /api/farmer/products/{id}/on-sale`
  - `ProductService.createProduct(Long actorId, ProductCreateRequest request)`
  - `ProductService.updateProduct(Long actorId, boolean admin, Long productId, ProductUpdateRequest request)`
  - `ProductService.changeStatus(Long actorId, boolean admin, Long productId, ProductStatus status)`

- [ ] **Step 1: Write failing authorization and management tests**

Create tests that:

- Register/login a `USER`; `POST /api/farmer/products` returns 403.
- Seed/login a `FARMER`; `POST /api/farmer/products` returns created product.
- A second farmer cannot update the first farmer's product; expect 403/code 1004.
- `ADMIN` can off-sale any product.

Use JWT tokens from existing auth endpoints, same style as `AuthFlowIntegrationTest`.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
cd backend
mvn -Dtest=FarmerProductApiTest test
```

Expected: FAIL because management API does not exist.

- [ ] **Step 3: Add request DTO validation**

`ProductCreateRequest` and `ProductUpdateRequest`:

```java
public record ProductCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotBlank @Size(max = 100) String originPlace,
        @NotBlank @Size(max = 500) String imageUrl
) {}
```

Use the same fields for `ProductUpdateRequest`.

- [ ] **Step 4: Implement controller**

Controller method pattern:

```java
@PostMapping
public ApiResponse<ProductDetailView> create(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                             @Valid @RequestBody ProductCreateRequest request) {
    return ApiResponse.ok(productService.createProduct(principal.userId(), request));
}
```

For update/status methods, compute admin:

```java
boolean admin = principal.roles().contains("ADMIN");
```

- [ ] **Step 5: Implement ownership checks**

In `ProductService`:

```java
private void assertCanManage(JwtService.JwtPrincipal principal, Product product) {
    boolean admin = principal.roles().contains("ADMIN");
    if (!admin && !product.getFarmerId().equals(principal.userId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
```

For `createProduct`, category must exist:

```java
if (categoryMapper.selectById(request.categoryId()) == null) {
    throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
}
```

- [ ] **Step 6: Run tests and commit**

Run:

```powershell
cd backend
mvn -Dtest=FarmerProductApiTest test
```

Expected: PASS.

Commit:

```bash
git add backend/src/main/java/com/agromall/product backend/src/test/java/com/agromall/product/FarmerProductApiTest.java
git commit -m "feat: add farmer product management APIs"
```

### Task 4: Frontend Product API and Store

**Files:**
- Create: `frontend/src/api/products.ts`
- Create: `frontend/src/stores/products.ts`
- Create: `frontend/src/stores/products.spec.ts`

**Interfaces:**
- Consumes: backend JSON routes from Tasks 2 and 3.
- Produces:
  - `listCategories(): Promise<Category[]>`
  - `listProducts(params?: ProductQuery): Promise<ProductSummary[]>`
  - `getProduct(id: number): Promise<ProductDetail>`
  - Pinia store `useProductStore`

- [ ] **Step 1: Write failing store test**

Create `products.spec.ts` that mocks `../api/products` and verifies:

```ts
it('loads categories and products', async () => {
  api.listCategories.mockResolvedValue([{ id: 1, name: '水果' }])
  api.listProducts.mockResolvedValue([{ id: 1, name: '洛川苹果', price: 29.9, stock: 100, originPlace: '陕西洛川', imageUrl: 'https://example.com/apple.jpg', categoryId: 1, categoryName: '水果' }])

  const store = useProductStore()
  await store.loadCatalog()

  expect(store.categories).toHaveLength(1)
  expect(store.products[0].name).toBe('洛川苹果')
})
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
cd frontend
npm run test -- --run src/stores/products.spec.ts
```

Expected: FAIL because store/API do not exist.

- [ ] **Step 3: Add API module**

`frontend/src/api/products.ts`:

```ts
import http from './http'

export interface Category { id: number; name: string }
export interface ProductSummary {
  id: number
  categoryId: number
  categoryName: string
  name: string
  price: number
  stock: number
  originPlace: string
  imageUrl: string
}
export interface ProductDetail extends ProductSummary {
  farmerId: number
  description: string
  status: 'ON_SALE' | 'OFF_SALE'
}
export interface ProductPayload {
  categoryId: number
  name: string
  description: string
  price: number
  stock: number
  originPlace: string
  imageUrl: string
}
export interface ProductQuery { keyword?: string; categoryId?: number }

export async function listCategories() {
  const { data } = await http.get('/categories')
  return data.data as Category[]
}
export async function listProducts(params: ProductQuery = {}) {
  const { data } = await http.get('/products', { params })
  return data.data as ProductSummary[]
}
export async function getProduct(id: number) {
  const { data } = await http.get(`/products/${id}`)
  return data.data as ProductDetail
}
export async function createProduct(payload: ProductPayload) {
  const { data } = await http.post('/farmer/products', payload)
  return data.data as ProductDetail
}
```

- [ ] **Step 4: Add product store**

`frontend/src/stores/products.ts`:

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/products'

export const useProductStore = defineStore('products', () => {
  const categories = ref<api.Category[]>([])
  const products = ref<api.ProductSummary[]>([])
  const currentProduct = ref<api.ProductDetail | null>(null)
  const keyword = ref('')
  const categoryId = ref<number | undefined>()

  async function loadCatalog() {
    categories.value = await api.listCategories()
    products.value = await api.listProducts({ keyword: keyword.value || undefined, categoryId: categoryId.value })
  }

  async function selectCategory(id?: number) {
    categoryId.value = id
    products.value = await api.listProducts({ keyword: keyword.value || undefined, categoryId: id })
  }

  async function search(value: string) {
    keyword.value = value
    products.value = await api.listProducts({ keyword: value || undefined, categoryId: categoryId.value })
  }

  async function loadProduct(id: number) {
    currentProduct.value = await api.getProduct(id)
  }

  return { categories, products, currentProduct, keyword, categoryId, loadCatalog, selectCategory, search, loadProduct }
})
```

- [ ] **Step 5: Run test and commit**

Run:

```powershell
cd frontend
npm run test -- --run src/stores/products.spec.ts
```

Expected: PASS.

Commit:

```bash
git add frontend/src/api/products.ts frontend/src/stores/products.ts frontend/src/stores/products.spec.ts
git commit -m "feat: add product frontend store"
```

### Task 5: Mall Home Page with Search and Category Filter

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/router/index.ts` only if route auth needs relaxing for public browsing.

**Interfaces:**
- Consumes: `useProductStore.loadCatalog`, `useProductStore.search`, `useProductStore.selectCategory`.
- Produces: Mall home page with public product browsing.

- [ ] **Step 1: Update route access expectation**

Decide route behavior:

- `/` should be public so visitors can browse products.
- Logged-in session controls whether header shows username/logout.
- Auth-only routes remain farmer form routes.

Modify home route:

```ts
{ path: '/', name: 'home', component: HomeView }
```

- [ ] **Step 2: Implement HomeView**

Use `onMounted(() => productStore.loadCatalog())`. The template contains:

```vue
<input v-model="searchText" placeholder="搜索农产品，如苹果、茶叶" @keyup.enter="productStore.search(searchText)" />
<button @click="productStore.selectCategory(undefined)">全部</button>
<button v-for="category in productStore.categories" :key="category.id" @click="productStore.selectCategory(category.id)">
  {{ category.name }}
</button>
<RouterLink v-for="product in productStore.products" :key="product.id" :to="{ name: 'product-detail', params: { id: product.id } }">
  <img :src="product.imageUrl" :alt="product.name" />
  <h3>{{ product.name }}</h3>
  <p>￥{{ product.price }}</p>
  <p>{{ product.originPlace }} · 库存 {{ product.stock }}</p>
</RouterLink>
```

- [ ] **Step 3: Add styling**

Add CSS classes:

```css
.catalog-hero { padding: 32px; border-radius: 28px; background: linear-gradient(135deg, #f0f9e8, #fff8df); }
.catalog-toolbar { display: flex; gap: 12px; flex-wrap: wrap; margin: 24px 0; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 18px; }
.product-card { background: #fff; border-radius: 20px; padding: 14px; box-shadow: 0 12px 30px rgba(39, 92, 54, 0.10); color: inherit; text-decoration: none; }
.product-card img { width: 100%; height: 150px; object-fit: cover; border-radius: 16px; }
.price { color: #e85d04; font-weight: 800; }
```

- [ ] **Step 4: Verify and commit**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/HomeView.vue frontend/src/styles.css frontend/src/router/index.ts
git commit -m "feat: show product catalog home"
```

### Task 6: Product Detail and Farmer Product Form

**Files:**
- Create: `frontend/src/views/ProductDetailView.vue`
- Create: `frontend/src/views/farmer/ProductFormView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/api/products.ts`

**Interfaces:**
- Consumes: `getProduct`, `createProduct`.
- Produces:
  - `/products/:id`
  - `/farmer/products/new`

- [ ] **Step 1: Add routes**

```ts
{
  path: '/products/:id',
  name: 'product-detail',
  component: ProductDetailView
},
{
  path: '/farmer/products/new',
  name: 'farmer-product-new',
  component: ProductFormView,
  meta: { requiresAuth: true }
}
```

Import the two new views.

- [ ] **Step 2: Add product detail view**

The detail view loads `Number(route.params.id)` and renders:

```vue
<img :src="product.imageUrl" :alt="product.name" />
<h1>{{ product.name }}</h1>
<p class="price">￥{{ product.price }}</p>
<p>{{ product.categoryName }} · {{ product.originPlace }} · 库存 {{ product.stock }}</p>
<p>{{ product.description }}</p>
```

- [ ] **Step 3: Add farmer create form**

The form fields:

- category select
- name
- description
- price
- stock
- originPlace
- imageUrl

Submit calls:

```ts
await createProduct(form)
await router.push({ name: 'home' })
```

- [ ] **Step 4: Verify and commit**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/ProductDetailView.vue frontend/src/views/farmer/ProductFormView.vue frontend/src/router/index.ts frontend/src/api/products.ts
git commit -m "feat: add product detail and farmer form"
```

### Task 7: Phase 2 Acceptance Verification and Documentation

**Files:**
- Create: `backend/src/test/java/com/agromall/product/ProductFlowIntegrationTest.java`
- Create: `docs/testing/phase-2-product-catalog.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: all Phase 2 APIs and frontend views.
- Produces: final acceptance evidence for product catalog phase.

- [ ] **Step 1: Write end-to-end backend acceptance test**

`ProductFlowIntegrationTest` should:

- Register/login a farmer test account.
- Create a product through `POST /api/farmer/products`.
- Call `GET /api/products` and find it.
- Call `GET /api/products/{id}` and verify detail.
- Off-sale the product.
- Verify public detail now returns product-not-found.

- [ ] **Step 2: Run backend acceptance**

Run:

```powershell
cd backend
mvn -Dtest=ProductFlowIntegrationTest test
```

Expected: PASS.

- [ ] **Step 3: Update docs**

Create `docs/testing/phase-2-product-catalog.md` with:

```markdown
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

## Manual checklist

1. Open the home page and confirm categories load.
2. Search for a seeded or newly created product.
3. Filter by category.
4. Open product detail.
5. Log in as farmer/admin and create a product.
6. Confirm ordinary user cannot open product publishing API.
7. Take a product off sale and confirm public listing hides it.
```

Update `README.md` tests section to include `docs/testing/phase-2-product-catalog.md`.

- [ ] **Step 4: Run full verification**

Run:

```powershell
cd backend
mvn clean verify

cd ..\frontend
npm run test -- --run
npm run build
```

Expected:

- Backend: all tests pass, `BUILD SUCCESS`.
- Frontend: all tests pass, build succeeds.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/agromall/product/ProductFlowIntegrationTest.java docs/testing/phase-2-product-catalog.md README.md
git commit -m "test: verify product catalog phase"
```

## Plan Self-Review

- Spec coverage: schema, categories, product fields, image URL strategy, permissions, public APIs, management APIs, frontend pages, error handling, tests, and future exclusions are covered.
- Placeholder scan: no unfinished placeholder markers or vague handling instructions remain.
- Type consistency: task APIs use `CategoryView`, `ProductSummaryView`, `ProductDetailView`, `ProductCreateRequest`, `ProductUpdateRequest`, `ProductStatus`, and `ProductService` consistently across backend and frontend.
