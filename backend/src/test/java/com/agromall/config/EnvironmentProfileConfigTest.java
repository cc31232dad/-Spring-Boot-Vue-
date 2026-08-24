package com.agromall.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentProfileConfigTest {

    @Test
    void testProfileUsesIsolatedDatabaseRedisAndUploadBoundaries() {
        Properties dev = load("application-dev.yml");
        Properties test = load("application-test.yml");

        assertThat(test.getProperty("agromall.environment")).isEqualTo("test");
        assertThat(test.getProperty("spring.datasource.url")).contains("agromall_test");
        assertThat(test.getProperty("agromall.redis.key-prefix")).isNotEqualTo(dev.getProperty("agromall.redis.key-prefix"));
        assertThat(test.getProperty("agromall.upload.product-dir")).isNotEqualTo(dev.getProperty("agromall.upload.product-dir"));
    }

    @Test
    void productionSecretsHaveNoDevelopmentFallbacks() {
        Properties prod = load("application-prod.yml");

        assertThat(prod.getProperty("spring.datasource.url")).contains("${AGROMALL_DB_URL}");
        assertThat(prod.getProperty("spring.datasource.password")).contains("${AGROMALL_DB_PASSWORD}");
        assertThat(prod.getProperty("agromall.jwt.secret")).contains("${AGROMALL_JWT_SECRET}");
        assertThat(prod.getProperty("agromall.upload.product-dir")).contains("${AGROMALL_UPLOAD_PRODUCT_DIR}");
    }

    private Properties load(String filename) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(filename));
        Properties properties = factory.getObject();
        assertThat(properties).as(filename + " must exist").isNotNull();
        return properties;
    }
}
