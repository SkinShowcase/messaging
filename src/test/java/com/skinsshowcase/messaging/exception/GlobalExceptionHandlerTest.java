package com.skinsshowcase.messaging.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestApi())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void userNotFound_returns404() throws Exception {
        mockMvc.perform(get("/__t/missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/__t/bad-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void methodArgumentNotValid_returns400() throws Exception {
        var result = mockMvc.perform(post("/__t/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("text");
    }

    @RestController
    static class TestApi {

        @GetMapping("/__t/missing-user")
        void missing() {
            throw new UserNotFoundException("nobody");
        }

        @GetMapping("/__t/bad-arg")
        void bad() {
            throw new IllegalArgumentException("invalid");
        }

        record Body(@NotBlank String text) {
        }

        @PostMapping("/__t/valid")
        void valid(@Valid @RequestBody Body body) {
        }
    }
}
