package com.agromall.auth.api;

import com.agromall.common.api.ApiResponse;
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
class AuthorizationApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousCannotReadSession() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void userCanReadOwnSession() throws Exception {
        String token = registerAndLogin("sessionuser", "13800000002");

        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("sessionuser"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    void userRoleCannotAccessFarmerEndpoints() throws Exception {
        String token = registerAndLogin("plainuserfarmer", "13800000003");

        mvc.perform(get("/api/farmer/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void userRoleCannotAccessAdminEndpoints() throws Exception {
        String token = registerAndLogin("plainuseradmin", "13800000004");

        mvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
    }

    private String registerAndLogin(String username, String phone) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Passw0rd!","phone":"%s"}
                                """.formatted(username, phone)))
                .andExpect(status().isOk());

        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Passw0rd!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @TestConfiguration
    static class AuthorizationTestEndpoints {

        @Bean
        FarmerTestController farmerTestController() {
            return new FarmerTestController();
        }

        @Bean
        AdminTestController adminTestController() {
            return new AdminTestController();
        }
    }

    @RestController
    @RequestMapping("/api/farmer")
    static class FarmerTestController {

        @GetMapping("/ping")
        ApiResponse<String> ping() {
            return ApiResponse.ok("farmer");
        }
    }

    @RestController
    @RequestMapping("/api/admin")
    static class AdminTestController {

        @GetMapping("/ping")
        ApiResponse<String> ping() {
            return ApiResponse.ok("admin");
        }
    }
}
