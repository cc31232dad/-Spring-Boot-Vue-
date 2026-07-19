package com.agromall.order;

import com.agromall.order.domain.Order;
import com.agromall.order.infrastructure.OrderMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class OrderPermissionApiTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private User shopperOne;
    private User shopperTwo;
    private User farmerOne;
    private User farmerTwo;
    private String shopperOneToken;
    private String shopperTwoToken;
    private String farmerOneToken;
    private String farmerTwoToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        shopperOne = createUser("permissionbuyerone", "13900000301", "USER");
        shopperTwo = createUser("permissionbuyertwo", "13900000302", "USER");
        farmerOne = createUser("permissionfarmerone", "13900000303", "FARMER");
        farmerTwo = createUser("permissionfarmertwo", "13900000304", "FARMER");
        createUser("permissionadmin", "13900000305", "ADMIN");
        shopperOneToken = login("permissionbuyerone");
        shopperTwoToken = login("permissionbuyertwo");
        farmerOneToken = login("permissionfarmerone");
        farmerTwoToken = login("permissionfarmertwo");
        adminToken = login("permissionadmin");
    }

    @Test
    void shopperCannotSeeAnotherShoppersOrder() throws Exception {
        Order order = insertOrder(shopperOne, farmerOne);

        mvc.perform(get("/api/orders/{id}", order.getId()).header("Authorization", bearer(shopperTwoToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1011));
    }

    @Test
    void farmerSeesOnlyOrdersAssignedToTheirUserId() throws Exception {
        Order assigned = insertOrder(shopperOne, farmerOne);
        insertOrder(shopperTwo, farmerTwo);

        mvc.perform(get("/api/farmer/orders").header("Authorization", bearer(farmerOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(assigned.getId()))
                .andExpect(jsonPath("$.data[0].farmerId").value(farmerOne.getId()));
    }

    @Test
    void secondFarmerCannotShipAnotherFarmersOrder() throws Exception {
        Order order = insertOrder(shopperOne, farmerOne);

        mvc.perform(patch("/api/farmer/orders/{id}/ship", order.getId()).header("Authorization", bearer(farmerTwoToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1011));
    }

    @Test
    void assignedFarmerCanShipPendingShipmentOrder() throws Exception {
        Order order = insertOrder(shopperOne, farmerOne);

        mvc.perform(patch("/api/farmer/orders/{id}/ship", order.getId()).header("Authorization", bearer(farmerOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void shopperCanCompleteShippedOrder() throws Exception {
        Order order = insertOrder(shopperOne, farmerOne);
        ship(order);

        mvc.perform(patch("/api/orders/{id}/complete", order.getId()).header("Authorization", bearer(shopperOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void shopperCannotCancelShippedOrder() throws Exception {
        Order order = insertOrder(shopperOne, farmerOne);
        ship(order);

        mvc.perform(patch("/api/orders/{id}/cancel", order.getId()).header("Authorization", bearer(shopperOneToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1012));
    }

    @Test
    void adminCanListAllOrders() throws Exception {
        insertOrder(shopperOne, farmerOne);
        insertOrder(shopperTwo, farmerTwo);

        mvc.perform(get("/api/admin/orders").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private User createUser(String username, String phone, String roleCode) {
        User user = User.create(username, passwordEncoder.encode(PASSWORD), phone);
        userMapper.insert(user);
        Role role = roleMapper.selectList(null).stream()
                .filter(candidate -> roleCode.equals(candidate.getCode()))
                .findFirst().orElseThrow();
        userRoleMapper.insert(user.getId(), role.getId());
        return user;
    }

    private Order insertOrder(User buyer, User farmer) {
        Order order = Order.create("PERM" + System.nanoTime(), buyer.getId(), farmer.getId(),
                new BigDecimal("12.50"), "Li Lei", "13800000000", "Xi'an");
        orderMapper.insert(order);
        return order;
    }

    private void ship(Order order) {
        order.setStatus("SHIPPED");
        orderMapper.updateById(order);
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
