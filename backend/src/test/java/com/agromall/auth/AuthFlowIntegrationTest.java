package com.agromall.auth;

import com.agromall.common.api.ApiResponse;
import com.agromall.user.infrastructure.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AuthFlowIntegrationTest {

    private static final String USERNAME = "acceptanceuser";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserMapper userMapper;

    @Test
    void registersLogsInReadsSessionRejectsFarmerAccessAndHashesPassword() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","phone":"13800000007"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));

        String token = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME));

        mvc.perform(get("/api/farmer/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));

        String storedHash = userMapper.selectByUsername(USERNAME).orElseThrow().getPasswordHash();
        assertThat(storedHash).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @TestConfiguration
    static class TestEndpoints {

        @Bean
        FarmerTestController farmerTestController() {
            return new FarmerTestController();
        }
    }

    @RestController
    @RequestMapping("/api/farmer")
    static class FarmerTestController {

        @GetMapping("/test")
        ApiResponse<String> test() {
            return ApiResponse.ok("farmer");
        }
    }
}
