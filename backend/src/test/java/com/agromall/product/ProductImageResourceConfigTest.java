package com.agromall.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "agromall.jwt.secret=0123456789abcdef0123456789abcdef",
        "agromall.jwt.access-token-minutes=30"
})
class ProductImageResourceConfigTest {

    @Autowired
    private MockMvc mvc;

    private final Path root = Path.of("uploads/products").toAbsolutePath().normalize();
    private final Path fixture = root.resolve("resource-fixture.txt");

    @AfterEach
    void cleanFixture() throws Exception {
        Files.deleteIfExists(fixture);
    }

    @Test
    void servesFilesFromConfiguredProductDirectory() throws Exception {
        Files.createDirectories(root);
        Files.writeString(fixture, "fixture");

        mvc.perform(get("/uploads/products/resource-fixture.txt"))
                .andExpect(status().isOk());
    }

    @Test
    void doesNotServePathTraversalOutsideProductDirectory() throws Exception {
        mvc.perform(get("/uploads/products/../application.yml"))
                .andExpect(status().is4xxClientError());
    }
}
