package com.agromall.product;

import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "agromall.jwt.secret=0123456789abcdef0123456789abcdef",
        "agromall.jwt.access-token-minutes=30"
})
class ProductFlowIntegrationTest {

    private static final String USERNAME = "catalogflowfarmer";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Test
    void farmerSubmitsProductAndPublicCatalogHidesItPendingReview() throws Exception {
        registerFarmer();
        String token = login();

        String createResponse = mvc.perform(post("/api/farmer/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":1,"name":"Acceptance peaches","description":"Sweet peaches for catalog acceptance","price":18.80,"stock":25,"originPlace":"Shandong","imageUrl":"https://example.com/acceptance-peach.jpg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acceptance peaches"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long productId = Long.valueOf(createResponse.replaceFirst(".*\\\"id\\\":(\\d+).*", "$1"));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)].name".formatted(productId),
                        not(hasItem("Acceptance peaches"))));

        mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1006));
    }

    private void registerFarmer() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","phone":"13900000031"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk());

        User farmer = userMapper.selectByUsername(USERNAME).orElseThrow();
        Role farmerRole = roleMapper.selectList(null).stream()
                .filter(role -> "FARMER".equals(role.getCode()))
                .findFirst()
                .orElseThrow();
        userRoleMapper.insert(farmer.getId(), farmerRole.getId());
    }

    private String login() throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
}
