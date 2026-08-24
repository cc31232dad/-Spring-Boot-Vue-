package com.agromall.product;

import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "agromall.jwt.secret=0123456789abcdef0123456789abcdef",
        "agromall.jwt.access-token-minutes=30"
})
class FarmerProductApiTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User firstFarmer;
    private String firstFarmerToken;
    private String secondFarmerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        firstFarmer = createUserWithRole("farmerone", "13900000021", "FARMER");
        createUserWithRole("farmertwo", "13900000022", "FARMER");
        createUserWithRole("catalogadmin", "13900000023", "ADMIN");
        firstFarmerToken = login("farmerone");
        secondFarmerToken = login("farmertwo");
        adminToken = login("catalogadmin");
    }

    @Test
    void userCannotCreateProduct() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"regularuser","password":"Passw0rd!","phone":"13900000024"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/farmer/products")
                        .header("Authorization", "Bearer " + login("regularuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("User product")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void farmerCreatesProduct() throws Exception {
        mvc.perform(post("/api/farmer/products")
                        .header("Authorization", "Bearer " + firstFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Fresh apples")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Fresh apples"))
                .andExpect(jsonPath("$.data.farmerId").value(firstFarmer.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    void farmerListsOnlyOwnedProductsWithReviewState() throws Exception {
        Product own = Product.create(1L, firstFarmer.getId(), "我的待审苹果", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(own);
        Product other = Product.create(1L, 999999L, "其他农户商品", "Fresh pears",
                new BigDecimal("9.50"), 20, "Shandong", "https://example.com/pear.jpg");
        other.setFarmerId(userMapper.selectByUsername("farmertwo").orElseThrow().getId());
        productMapper.insert(other);

        mvc.perform(get("/api/farmer/products")
                        .header("Authorization", "Bearer " + firstFarmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)].name".formatted(own.getId())).value("我的待审苹果"))
                .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(other.getId())).isEmpty());
    }

    @Test
    void farmerCreatesProductWithRootRelativeImageUrl() throws Exception {
        mvc.perform(post("/api/farmer/products")
                        .header("Authorization", "Bearer " + firstFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Local image apples").replace("https://example.com/apple.jpg", "/images/apple.jpg")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value("/images/apple.jpg"));
    }

    @Test
    void farmerCannotCreateProductWithInvalidImageUrl() throws Exception {
        mvc.perform(post("/api/farmer/products")
                        .header("Authorization", "Bearer " + firstFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Invalid image apples").replace("https://example.com/apple.jpg", "not-a-url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void anotherFarmerCannotUpdateProduct() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Owned apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);

        mvc.perform(put("/api/farmer/products/{id}", product.getId())
                        .header("Authorization", "Bearer " + secondFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Changed apples")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void farmerCannotUpdateProductWithInvalidImageUrl() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Owned apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);

        mvc.perform(put("/api/farmer/products/{id}", product.getId())
                        .header("Authorization", "Bearer " + firstFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Changed apples").replace("https://example.com/apple.jpg", "not-a-url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void farmerUpdateResubmitsRejectedProductAndClearsReviewAudit() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Rejected apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);
        jdbcTemplate.update("""
                UPDATE product
                SET status = 'REJECTED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP,
                    review_reason = '图片不清晰'
                WHERE id = ?
                """, firstFarmer.getId(), product.getId());

        mvc.perform(put("/api/farmer/products/{id}", product.getId())
                        .header("Authorization", "Bearer " + firstFarmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Updated apples")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        var audit = jdbcTemplate.queryForMap("""
                SELECT reviewed_by, reviewed_at, review_reason
                FROM product
                WHERE id = ?
                """, product.getId());
        org.assertj.core.api.Assertions.assertThat(audit.get("reviewed_by")).isNull();
        org.assertj.core.api.Assertions.assertThat(audit.get("reviewed_at")).isNull();
        org.assertj.core.api.Assertions.assertThat(audit.get("review_reason")).isNull();
    }

    @Test
    void farmerOnSaleEndpointResubmitsProductForReview() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Resubmitted apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        product.offSale();
        productMapper.insert(product);

        mvc.perform(patch("/api/farmer/products/{id}/on-sale", product.getId())
                        .header("Authorization", "Bearer " + firstFarmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    void farmerCannotChangePendingProductStatus() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Pending apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);

        mvc.perform(patch("/api/farmer/products/{id}/off-sale", product.getId())
                        .header("Authorization", "Bearer " + firstFarmerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1023));

        mvc.perform(patch("/api/farmer/products/{id}/on-sale", product.getId())
                        .header("Authorization", "Bearer " + firstFarmerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1023));
    }
    @Test
    void adminCanOffSaleAnyProduct() throws Exception {
        Product product = Product.create(1L, firstFarmer.getId(), "Admin apples", "Fresh apples",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);

        mvc.perform(patch("/api/farmer/products/{id}/off-sale", product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF_SALE"));
    }

    private User createUserWithRole(String username, String phone, String roleCode) {
        User user = User.create(username, passwordEncoder.encode(PASSWORD), phone);
        userMapper.insert(user);
        Role role = roleMapper.selectList(null).stream()
                .filter(candidate -> roleCode.equals(candidate.getCode()))
                .findFirst()
                .orElseThrow();
        userRoleMapper.insert(user.getId(), role.getId());
        return user;
    }

    private String login(String username) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String productRequest(String name) {
        return """
                {"categoryId":1,"name":"%s","description":"Fresh seasonal produce","price":12.50,"stock":30,"originPlace":"Shaanxi","imageUrl":"https://example.com/apple.jpg"}
                """.formatted(name);
    }
}
