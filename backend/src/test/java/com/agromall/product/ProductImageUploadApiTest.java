package com.agromall.product;

import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
class ProductImageUploadApiTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Autowired private MockMvc mvc;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private String farmerToken;
    private String buyerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        farmerToken = login(createUserWithRole("imagefarmer", "13900000801", "FARMER"));
        buyerToken = login(createUserWithRole("imagebuyer", "13900000802", "USER"));
        adminToken = login(createUserWithRole("imageadmin", "13900000803", "ADMIN"));
    }

    @Test
    void farmerCanUploadPngAndReceivesStoredUrl() throws Exception {
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "apple.png",
                                MediaType.IMAGE_PNG_VALUE, PNG))
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/products/")));
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "empty.png",
                                MediaType.IMAGE_PNG_VALUE, new byte[0]))
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void unsupportedContentIsRejected() throws Exception {
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "notes.txt",
                                MediaType.TEXT_PLAIN_VALUE, "not an image".getBytes()))
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void oversizedFileIsRejected() throws Exception {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "large.png",
                                MediaType.IMAGE_PNG_VALUE, oversized))
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void imageExtensionAndContentMustMatch() throws Exception {
        byte[] renamedText = Arrays.copyOf("not a png".getBytes(), 9);
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "fake.png",
                                MediaType.IMAGE_PNG_VALUE, renamedText))
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void declaredMimeTypeMustMatchImageExtension() throws Exception {
        mvc.perform(multipart("/api/farmer/product-images")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "fake.png",
                                MediaType.TEXT_PLAIN_VALUE, PNG))
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void buyerAndAdminCannotUploadImages() throws Exception {
        for (String token : new String[]{buyerToken, adminToken}) {
            mvc.perform(multipart("/api/farmer/product-images")
                            .file(new org.springframework.mock.web.MockMultipartFile("file", "apple.png",
                                    MediaType.IMAGE_PNG_VALUE, PNG))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(1004));
        }
    }

    private User createUserWithRole(String username, String phone, String roleCode) {
        User user = User.create(username, passwordEncoder.encode(PASSWORD), phone);
        userMapper.insert(user);
        Role role = roleMapper.selectList(null).stream()
                .filter(candidate -> roleCode.equals(candidate.getCode()))
                .findFirst()
                .orElseThrow();
        userRoleMapper.insert(user.getId(), role.getId());
        return user;
    }

    private String login(User user) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(user.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
}
