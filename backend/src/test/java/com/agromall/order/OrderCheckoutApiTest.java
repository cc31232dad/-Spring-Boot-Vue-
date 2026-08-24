package com.agromall.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
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
class OrderCheckoutApiTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private ObjectMapper objectMapper;

    private String buyerToken;
    private User farmerOne;
    private User farmerTwo;

    @BeforeEach
    void setUp() throws Exception {
        register("orderbuyer", "13900000201");
        register("orderfarmerone", "13900000202");
        register("orderfarmertwo", "13900000203");
        buyerToken = login("orderbuyer");
        farmerOne = userMapper.selectByUsername("orderfarmerone").orElseThrow();
        farmerTwo = userMapper.selectByUsername("orderfarmertwo").orElseThrow();
    }

    @Test
    void checkoutSelectedCartItemsCreatesOneOrderAndDeductsStockAndClearsCart() throws Exception {
        Product product = insertProduct(farmerOne, "Checkout apples", 10);
        long cartItemId = addCartItem(product.getId(), 2);

        mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].farmerId").value(farmerOne.getId()))
                .andExpect(jsonPath("$.data[0].items[0].quantity").value(2));

        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(8);
        assertThat(orderMapper.selectCount(null)).isEqualTo(1);
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void checkoutItemsFromTwoFarmersCreatesTwoOrders() throws Exception {
        Product first = insertProduct(farmerOne, "Farmer one apples", 10);
        Product second = insertProduct(farmerTwo, "Farmer two pears", 10);
        long firstItem = addCartItem(first.getId(), 1);
        long secondItem = addCartItem(second.getId(), 1);

        mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(firstItem, secondItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        assertThat(orderMapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void insufficientStockRollsBackCheckoutWithoutChangingCartStockOrOrders() throws Exception {
        Product product = insertProduct(farmerOne, "Limited checkout apples", 2);
        long cartItemId = addCartItem(product.getId(), 2);
        product.setStock(1);
        productMapper.updateById(product);

        mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartItemId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1010));

        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(1);
        assertThat(orderMapper.selectCount(null)).isZero();
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void atomicStockDeductionRequiresProductStillOnSale() {
        Product product = insertProduct(farmerOne, "Atomic off-sale apples", 5);
        product.offSale();
        productMapper.updateById(product);

        assertThat(productMapper.deductStock(product.getId(), 1)).isZero();
        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(5);
    }

    @Test
    void checkoutOfAlreadyCheckedOutCartItemFailsWithoutCreatingAnotherOrder() throws Exception {
        Product product = insertProduct(farmerOne, "Single-use cart apples", 4);
        long cartItemId = addCartItem(product.getId(), 1);

        checkoutOrder(cartItemId);

        mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartItemId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1008));

        assertThat(orderMapper.selectCount(null)).isEqualTo(1);
        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(3);
    }

    @Test
    void orderSnapshotsDoNotMakeOffSaleProductPublicAgain() throws Exception {
        Product product = insertProduct(farmerOne, "Snapshot apples", 4);
        long cartItemId = addCartItem(product.getId(), 1);

        mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartItemId)))
                .andExpect(status().isOk());

        product = productMapper.selectById(product.getId());
        product.offSale();
        productMapper.updateById(product);

        mvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void buyerCanListViewAndCancelPendingOrderWhichRestoresStock() throws Exception {
        Product product = insertProduct(farmerOne, "Cancellable apples", 6);
        long orderId = checkoutOrder(addCartItem(product.getId(), 2));

        mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/orders/{id}", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId));
        mvc.perform(patch("/api/orders/{id}/cancel", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mvc.perform(patch("/api/orders/{id}/cancel", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1012));

        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(6);
    }

    @Test
    void onlyOneConditionalCancellationTransitionCanWin() throws Exception {
        Product product = insertProduct(farmerOne, "Transition-guard apples", 6);
        long orderId = checkoutOrder(addCartItem(product.getId(), 1));

        assertThat(orderMapper.markCancelledIfPending(orderId)).isOne();
        assertThat(orderMapper.markCancelledIfPending(orderId)).isZero();
    }

    @Test
    void onlyOneConditionalCompletionTransitionCanWin() throws Exception {
        Product product = insertProduct(farmerOne, "Completion-guard apples", 6);
        long orderId = checkoutOrder(addCartItem(product.getId(), 1));
        com.agromall.order.domain.Order order = orderMapper.selectById(orderId);
        order.setStatus("SHIPPED");
        orderMapper.updateById(order);

        assertThat(orderMapper.markCompletedIfShipped(orderId)).isOne();
        assertThat(orderMapper.markCompletedIfShipped(orderId)).isZero();
    }

    @Test
    void buyerCanCompleteOnlyShippedOrder() throws Exception {
        Product product = insertProduct(farmerOne, "Shipped apples", 6);
        long orderId = checkoutOrder(addCartItem(product.getId(), 1));
        com.agromall.order.domain.Order order = orderMapper.selectById(orderId);
        order.setStatus("SHIPPED");
        orderMapper.updateById(order);

        mvc.perform(patch("/api/orders/{id}/complete", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
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
                .andReturn().getResponse().getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Product insertProduct(User farmer, String name, int stock) {
        Product product = Product.create(1L, farmer.getId(), name, "Fresh produce",
                new BigDecimal("12.50"), stock, "Shaanxi", "https://example.com/apple.jpg");
        product.approve(farmer.getId(), LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    private long addCartItem(Long productId, int quantity) throws Exception {
        String response = mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return readCartItemId(response, productId);
    }

    private String checkoutBody(long... cartItemIds) {
        String ids = java.util.Arrays.stream(cartItemIds).mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"cartItemIds\":[%s],\"receiverName\":\"Li Lei\",\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Xi'an\"}".formatted(ids);
    }

    private long checkoutOrder(long cartItemId) throws Exception {
        String response = mvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartItemId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return readLong(response, "/data/0/id");
    }

    private long readLong(String response, String jsonPointer) throws Exception {
        JsonNode node = objectMapper.readTree(response).at(jsonPointer);
        assertThat(node.isNumber())
                .as("Expected numeric JSON value at %s in response %s", jsonPointer, response)
                .isTrue();
        return node.asLong();
    }

    private long readCartItemId(String response, Long productId) throws Exception {
        JsonNode items = objectMapper.readTree(response).at("/data/items");
        assertThat(items.isArray())
                .as("Expected cart items array in response %s", response)
                .isTrue();
        for (JsonNode item : items) {
            if (item.path("productId").asLong() == productId) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("Expected cart item for product %d in response %s".formatted(productId, response));
    }
}
