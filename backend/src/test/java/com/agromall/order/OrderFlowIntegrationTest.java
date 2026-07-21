package com.agromall.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class OrderFlowIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shopperCompletesOneFarmerOrderAndCancelsAnotherRestoringStock() throws Exception {
        String shopperToken = registerAndLogin("orderflowshopper", "13900000301");
        String farmerOneToken = registerFarmerAndLogin("orderflowfarmerone", "13900000302");
        registerFarmerAndLogin("orderflowfarmertwo", "13900000303");
        User farmerOne = userMapper.selectByUsername("orderflowfarmerone").orElseThrow();
        User farmerTwo = userMapper.selectByUsername("orderflowfarmertwo").orElseThrow();
        Product apples = insertProduct(farmerOne, "Flow apples", 10);
        Product pears = insertProduct(farmerTwo, "Flow pears", 8);

        long appleCartItem = addCartItem(shopperToken, apples.getId(), 2);
        long pearCartItem = addCartItem(shopperToken, pears.getId(), 3);
        String checkoutResponse = mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", bearer(shopperToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(appleCartItem, pearCartItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        long appleOrderId = orderIdForFarmer(checkoutResponse, farmerOne.getId());
        long pearOrderId = orderIdForFarmer(checkoutResponse, farmerTwo.getId());
        assertThat(productMapper.selectById(apples.getId()).getStock()).isEqualTo(8);
        assertThat(productMapper.selectById(pears.getId()).getStock()).isEqualTo(5);

        mvc.perform(patch("/api/farmer/orders/{id}/ship", appleOrderId)
                        .header("Authorization", bearer(farmerOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
        mvc.perform(patch("/api/orders/{id}/complete", appleOrderId)
                        .header("Authorization", bearer(shopperToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mvc.perform(patch("/api/orders/{id}/cancel", pearOrderId)
                        .header("Authorization", bearer(shopperToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(productMapper.selectById(pears.getId()).getStock()).isEqualTo(8);
    }

    private String registerFarmerAndLogin(String username, String phone) throws Exception {
        String token = registerAndLogin(username, phone);
        User farmer = userMapper.selectByUsername(username).orElseThrow();
        Role farmerRole = roleMapper.selectList(null).stream()
                .filter(role -> "FARMER".equals(role.getCode()))
                .findFirst().orElseThrow();
        userRoleMapper.insert(farmer.getId(), farmerRole.getId());
        return login(username);
    }

    private String registerAndLogin(String username, String phone) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\"}"
                                .formatted(username, PASSWORD, phone)))
                .andExpect(status().isOk());
        return login(username);
    }

    private String login(String username) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Product insertProduct(User farmer, String name, int stock) {
        Product product = Product.create(1L, farmer.getId(), name, "Acceptance product",
                new BigDecimal("12.50"), stock, "Shaanxi", "https://example.com/product.jpg");
        productMapper.insert(product);
        return product;
    }

    private long addCartItem(String shopperToken, Long productId, int quantity) throws Exception {
        String response = mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(shopperToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceFirst(".*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private String checkoutBody(long... cartItemIds) {
        String ids = java.util.Arrays.stream(cartItemIds).mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"cartItemIds\":[%s],\"receiverName\":\"Li Lei\",\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Xi'an\"}"
                .formatted(ids);
    }

    private long orderIdForFarmer(String response, Long farmerId) throws Exception {
        for (JsonNode order : objectMapper.readTree(response).path("data")) {
            if (order.path("farmerId").asLong() == farmerId) {
                return order.path("id").asLong();
            }
        }
        throw new IllegalArgumentException("Checkout response does not contain an order for farmer " + farmerId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
