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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "agromall.jwt.secret=0123456789abcdef0123456789abcdef",
        "agromall.jwt.access-token-minutes=30"
})
class AdminProductReviewApiTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private User farmer;
    private String adminToken;
    private String farmerToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        farmer = createUserWithRole("reviewfarmer", "13900000121", "FARMER");
        createUserWithRole("reviewadmin", "13900000122", "ADMIN");
        createUserWithRole("reviewuser", "13900000123", "USER");
        adminToken = login("reviewadmin");
        farmerToken = login("reviewfarmer");
        userToken = login("reviewuser");
    }

    @Test
    void adminListsPendingProductsAndApprovesProductForPublicCatalog() throws Exception {
        Product product = pendingProduct("待审核苹果");

        mvc.perform(get("/api/admin/products/review")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)].name".formatted(product.getId()),
                        hasItem("待审核苹果")))
                .andExpect(jsonPath("$.data[?(@.id == %d)].status".formatted(product.getId()),
                        hasItem("PENDING_REVIEW")));

        mvc.perform(post("/api/admin/products/{id}/approve", product.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.reviewedBy").isNumber())
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        mvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("待审核苹果"));
    }

    @Test
    void adminRejectsProductAndPersistsReason() throws Exception {
        Product product = pendingProduct("拒绝苹果");

        mvc.perform(post("/api/admin/products/{id}/reject", product.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  图片不清晰  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewReason").value("图片不清晰"))
                .andExpect(jsonPath("$.data.reviewedBy").isNumber());
    }

    @Test
    void rejectRequiresNonBlankReason() throws Exception {
        Product product = pendingProduct("缺少原因苹果");

        mvc.perform(post("/api/admin/products/{id}/reject", product.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void cannotReviewProductTwice() throws Exception {
        Product product = pendingProduct("重复审核苹果");
        mvc.perform(post("/api/admin/products/{id}/approve", product.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/products/{id}/approve", product.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1023));
    }

    @Test
    void nonAdminsCannotUseReviewApi() throws Exception {
        Product product = pendingProduct("权限苹果");

        mvc.perform(get("/api/admin/products/review")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
        mvc.perform(post("/api/admin/products/{id}/approve", product.getId())
                        .header("Authorization", bearer(farmerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
        mvc.perform(get("/api/admin/products/review"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    private Product pendingProduct(String name) {
        Product product = Product.create(1L, farmer.getId(), name, "Fresh produce",
                new BigDecimal("12.50"), 30, "Shaanxi", "https://example.com/apple.jpg");
        productMapper.insert(product);
        return product;
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
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
