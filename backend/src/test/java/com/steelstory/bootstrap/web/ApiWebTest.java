package com.steelstory.bootstrap.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiWebTest {
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new StatusController(), new FailureController())
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void exposesPublicStatus() throws Exception {
        mvc.perform(get("/api/v1/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("steelstory"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unexpectedErrorsDoNotLeakInternalDetails() throws Exception {
        mvc.perform(get("/test/failure")).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void frameworkErrorsKeepTheirHttpStatus() throws Exception {
        mvc.perform(post("/api/v1/status")).andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @RestController
    static class FailureController {
        @GetMapping("/test/failure")
        String fail() {
             throw new IllegalStateException("private database detail"); 
            }
    }
}
