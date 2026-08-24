package com.agromall.product;

import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductCategory;
import com.agromall.product.infrastructure.ProductCategoryMapper;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductSchemaTest {

    @Autowired ProductCategoryMapper categoryMapper;
    @Autowired ProductMapper productMapper;
    @Autowired UserMapper userMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void seedsCategoriesAndPersistsProduct() {
        assertThat(categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder)))
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
        assertThat(saved.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(saved.getPrice()).isEqualByComparingTo("29.90");
        assertThat(saved.getStock()).isEqualTo(100);

        jdbcTemplate.update("""
                UPDATE product
                SET reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, review_reason = '信息不完整'
                WHERE id = ?
                """, farmer.getId(), product.getId());
        var review = jdbcTemplate.queryForMap("""
                SELECT reviewed_by, reviewed_at, review_reason
                FROM product
                WHERE id = ?
                """, product.getId());
        assertThat(((Number) review.get("reviewed_by")).longValue()).isEqualTo(farmer.getId());
        assertThat(review.get("reviewed_at")).isNotNull();
        assertThat(review.get("review_reason")).isEqualTo("信息不完整");
    }
}
