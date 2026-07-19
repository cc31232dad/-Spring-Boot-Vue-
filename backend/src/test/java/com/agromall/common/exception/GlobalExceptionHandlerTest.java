package com.agromall.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void mapsBusinessExceptionToConflict() throws Exception {
        mvc.perform(get("/test-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void mapsValidationErrorsToBadRequest() throws Exception {
        mvc.perform(post("/test-validation")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void mapsUnknownErrorsToInternalServerErrorWithoutStackTrace() throws Exception {
        mvc.perform(get("/test-unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(9999))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @RestController
    static class TestController {

        @GetMapping("/test-error")
        void fail() {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        @PostMapping("/test-validation")
        void validate(@Valid @RequestBody Request request) {
        }

        @GetMapping("/test-unknown")
        void unknown() {
            throw new IllegalStateException("unexpected");
        }
    }

    record Request(@NotBlank String name) {
    }
}
