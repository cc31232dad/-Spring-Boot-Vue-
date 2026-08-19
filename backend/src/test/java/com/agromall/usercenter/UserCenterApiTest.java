package com.agromall.usercenter;

import com.agromall.order.domain.Order;
import com.agromall.order.infrastructure.OrderMapper;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"agromall.jwt.secret=0123456789abcdef0123456789abcdef", "agromall.jwt.access-token-minutes=30"})
class UserCenterApiTest {
    private static final String PASSWORD = "Passw0rd!";
    @Autowired MockMvc mvc;
    @Autowired UserMapper userMapper;
    @Autowired RoleMapper roleMapper;
    @Autowired UserRoleMapper userRoleMapper;
    @Autowired ProductMapper productMapper;
    @Autowired OrderMapper orderMapper;
    @Autowired BCryptPasswordEncoder encoder;

    @Test
    void addressesAreOwnedAndOnlyOneDefaultRemains() throws Exception {
        User user = user("ucaddr" + System.nanoTime(), "139" + digits());
        User other = user("ucother" + System.nanoTime(), "138" + digits());
        String token = login(user.getUsername(), user.getPhone());
        String otherToken = login(other.getUsername(), other.getPhone());
        mvc.perform(post("/api/address").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(address("A", true))).andExpect(status().isOk());
        mvc.perform(post("/api/address").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(address("B", true))).andExpect(status().isOk());
        mvc.perform(get("/api/address").header("Authorization", bearer(token))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andExpect(jsonPath("$.data[1].isDefault").value(false));
        mvc.perform(get("/api/address").header("Authorization", bearer(otherToken))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void addingFavoriteIsIdempotent() throws Exception {
        User user = user("ucfav" + System.nanoTime(), "137" + digits());
        Product product = product(user);
        String token = login(user.getUsername(), user.getPhone());
        mvc.perform(post("/api/favorites/{id}", product.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/favorites/{id}", product.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(token))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].productId").value(product.getId()));
    }

    @Test
    void myOrdersCanFilterAndPageByStatus() throws Exception {
        User user = user("ucorder" + System.nanoTime(), "136" + digits());
        User farmer = user("ucfarmer" + System.nanoTime(), "135" + digits());
        String token = login(user.getUsername(), user.getPhone());
        Order pending = Order.create("UC" + System.nanoTime(), user.getId(), farmer.getId(), new BigDecimal("1.00"), "A", "13800000000", "X");
        orderMapper.insert(pending);
        Order done = Order.create("UC" + System.nanoTime(), user.getId(), farmer.getId(), new BigDecimal("2.00"), "A", "13800000000", "X");
        done.setStatus("COMPLETED"); orderMapper.insert(done);
        mvc.perform(get("/api/orders/my").param("status", "done").param("page", "0").param("size", "1")
                        .header("Authorization", bearer(token))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    private User user(String username, String phone) {
        User user = User.create(username, encoder.encode(PASSWORD), phone); userMapper.insert(user);
        Role role = roleMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Role>lambdaQuery().eq(Role::getCode, "USER"));
        userRoleMapper.insert(user.getId(), role.getId()); return user;
    }
    private Product product(User farmer) {
        Product product = Product.create(1L, farmer.getId(), "UC product", "desc", new BigDecimal("3.00"), 5, "Shaanxi", "https://example.com/p.jpg");
        product.approve(farmer.getId(), LocalDateTime.now()); productMapper.insert(product); return product;
    }
    private String login(String username, String phone) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
    private String address(String name, boolean isDefault) { return "{\"name\":\"%s\",\"phone\":\"13800000000\",\"province\":\"Shaanxi\",\"city\":\"Xian\",\"district\":\"Yanta\",\"detail\":\"No.1\",\"isDefault\":%s}".formatted(name, isDefault); }
    private String bearer(String token) { return "Bearer " + token; }
    private String digits() { return "%08d".formatted(Math.floorMod((int) System.nanoTime(), 100000000)); }
}
