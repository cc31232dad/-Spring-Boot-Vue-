package com.agromall.cart;

import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.cart.application.CartService;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;
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
class CartApiTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartService cartService;

    private String firstUserToken;
    private String secondUserToken;
    private User farmer;

    @BeforeEach
    void setUp() throws Exception {
        register("cartuserone", "13900000101");
        register("cartusertwo", "13900000102");
        register("cartfarmer", "13900000103");
        firstUserToken = login("cartuserone");
        secondUserToken = login("cartusertwo");
        farmer = userMapper.selectByUsername("cartfarmer").orElseThrow();
    }

    @Test
    void unauthenticatedUserCannotReadCart() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void userAddsOnSaleProductAndSeesItInCart() throws Exception {
        Product product = insertProduct("Cart apples", 10);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":2}".formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productName").value("Cart apples"))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(25.00))
                .andExpect(jsonPath("$.data.totalAmount").value(25.00));
    }

    @Test
    void addingSameProductAgainUpdatesQuantity() throws Exception {
        Product product = insertProduct("Repeat apples", 10);

        addItem(firstUserToken, product.getId(), 2);
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":3}".formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5));
    }

    @Test
    void userCannotUpdateAnotherUsersCartItem() throws Exception {
        Product product = insertProduct("Private apples", 10);
        String response = addItem(firstUserToken, product.getId(), 2);
        long itemId = Long.parseLong(response.replaceFirst(".*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1"));

        mvc.perform(put("/api/cart/items/{id}", itemId)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void deletingCartItemRemovesIt() throws Exception {
        Product product = insertProduct("Delete apples", 10);
        String response = addItem(firstUserToken, product.getId(), 2);
        long itemId = Long.parseLong(response.replaceFirst(".*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1"));

        mvc.perform(delete("/api/cart/items/{id}", itemId)
                        .header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    void userCannotAddOffSaleProduct() throws Exception {
        Product product = insertProduct("Off sale apples", 10);
        product.offSale();
        productMapper.updateById(product);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":2}".formatted(product.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1009));
    }

    @Test
    void userCannotAddQuantityGreaterThanStock() throws Exception {
        Product product = insertProduct("Limited apples", 10);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":11}".formatted(product.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    void userCannotUpdateQuantityGreaterThanStock() throws Exception {
        Product product = insertProduct("Update limited apples", 10);
        String response = addItem(firstUserToken, product.getId(), 1);
        long itemId = Long.parseLong(response.replaceFirst(".*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1"));

        mvc.perform(put("/api/cart/items/{id}", itemId)
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":11}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    void userCannotAccumulateQuantityPastStockWhenIntegerAdditionOverflows() throws Exception {
        Product product = insertProduct("Maximum stock apples", Integer.MAX_VALUE);
        addItem(firstUserToken, product.getId(), 1);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(product.getId(), Integer.MAX_VALUE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    void emptyCartItemListsAreSafeForCheckoutHelpers() {
        Long userId = userMapper.selectByUsername("cartuserone").orElseThrow().getId();

        assertThat(cartService.getOwnedItems(userId, java.util.List.of())).isEmpty();
        cartService.deleteItems(userId, java.util.List.of());
    }

    private void register(String username, String phone) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\"}"
                                .formatted(username, PASSWORD, phone)))
                .andExpect(status().isOk());
    }

    private String login(String username) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Product insertProduct(String name, int stock) {
        Product product = Product.create(1L, farmer.getId(), name, "Fresh produce",
                new BigDecimal("12.50"), stock, "Shaanxi", "https://example.com/apple.jpg");
        product.approve(farmer.getId(), LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    private String addItem(String token, Long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
