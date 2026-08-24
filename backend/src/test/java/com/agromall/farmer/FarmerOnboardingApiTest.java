package com.agromall.farmer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
class FarmerOnboardingApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void farmerApplicationIsPendingAndCannotLoginBeforeApproval() throws Exception {
        mvc.perform(post("/api/auth/farmer/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("13800000991")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.phone").value("13800000991"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"13800000991","password":"Passw0rd!"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1025));
    }

    @Test
    void loginRejectsARequestedRoleThatTheAccountDoesNotOwn() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rolebuyer\",\"password\":\"Passw0rd!\",\"phone\":\"13800000994\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rolebuyer\",\"password\":\"Passw0rd!\",\"role\":\"FARMER\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void adminCanApproveFarmerApplication() throws Exception {
        String response = mvc.perform(post("/api/auth/farmer/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("13800000992")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long farmerId = objectMapper.readTree(response).path("data").path("id").asLong();

        String adminToken = createAdminAndLogin();
        mvc.perform(get("/api/admin/farmers").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mvc.perform(post("/api/admin/farmers/{id}/approve", farmerId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"13800000992\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isString());
    }

    private String createAdminAndLogin() throws Exception {
        User user = User.create("onboardingadmin", passwordEncoder.encode("Passw0rd!"), "13800000993");
        userMapper.insert(user);
        Role admin = roleMapper.selectList(null).stream().filter(r -> "ADMIN".equals(r.getCode())).findFirst().orElseThrow();
        userRoleMapper.insert(user.getId(), admin.getId());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"onboardingadmin\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.path("data").path("accessToken").asText();
    }

    private String application(String phone) {
        return """
                {
                  "phone":"%s",
                  "password":"Passw0rd!",
                  "realName":"张三",
                  "idCard":"610102199001010011",
                  "province":"陕西省",
                  "city":"西安市",
                  "district":"雁塔区",
                  "detailAddress":"丈八街道示例村1号",
                  "category":"新鲜水果",
                  "licenseNo":""
                }
                """.formatted(phone);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
