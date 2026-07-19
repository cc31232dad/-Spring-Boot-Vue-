package com.agromall.product;

import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "agromall.jwt.secret=0123456789abcdef0123456789abcdef",
        "agromall.jwt.access-token-minutes=30"
})
class ProductCatalogApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    private Long onSaleProductId;
    private Long offSaleProductId;

    @BeforeEach
    void setUp() {
        User farmer = User.create("catalogfarmer", "hash", "13900000011");
        userMapper.insert(farmer);

        Product onSale = Product.create(1L, farmer.getId(), "洛川苹果", "脆甜红富士苹果",
                new BigDecimal("29.90"), 100, "陕西洛川", "https://example.com/apple.jpg");
        productMapper.insert(onSale);
        onSaleProductId = onSale.getId();

        Product offSale = Product.create(1L, farmer.getId(), "下架苹果", "暂不销售",
                new BigDecimal("19.90"), 20, "陕西洛川", "https://example.com/off-sale-apple.jpg");
        offSale.offSale();
        productMapper.insert(offSale);
        offSaleProductId = offSale.getId();
    }

    @Test
    void listsCategories() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("水果"));
    }

    @Test
    void listsOnlyOnSaleProductsMatchingKeywordAndCategory() throws Exception {
        mvc.perform(get("/api/products").param("keyword", "苹果").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("洛川苹果"))
                .andExpect(jsonPath("$.data[0].categoryName").value("水果"));
    }

    @Test
    void returnsPublicProductDetail() throws Exception {
        mvc.perform(get("/api/products/{id}", onSaleProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("洛川苹果"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
    }

    @Test
    void hidesOffSaleProductDetails() throws Exception {
        mvc.perform(get("/api/products/{id}", offSaleProductId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1006));
    }
}
