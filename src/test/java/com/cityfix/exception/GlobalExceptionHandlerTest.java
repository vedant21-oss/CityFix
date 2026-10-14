package com.cityfix.exception;

import com.cityfix.controller.ReportController;
import com.cityfix.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void unknownStatusParameterReturnsTheStandardFormat() throws Exception {
        mockMvc.perform(get("/api/reports").param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'FOO' for status"));
    }

    @Test
    void unknownCategoryParameterReturnsTheStandardFormat() throws Exception {
        mockMvc.perform(get("/api/reports").param("category", "SPACESHIP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'SPACESHIP' for category"));
    }

    @Test
    void nonNumericIdReturnsTheStandardFormat() throws Exception {
        mockMvc.perform(get("/api/reports/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for id"));
    }

    @Test
    void malformedJsonBodyReturnsTheStandardFormat() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void missingRequestBodyReturnsTheStandardFormat() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void unknownCategoryInTheBodyReturnsTheStandardFormat() throws Exception {
        String json = """
                {
                  "title": "Pothole",
                  "description": "Deep hole in the road",
                  "category": "SPACESHIP",
                  "area": "Downtown",
                  "reporterName": "Ana",
                  "reporterEmail": "ana@example.com"
                }
                """;

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'SPACESHIP' for category"));
    }

    @Test
    void errorMessagesDoNotExposeJavaDetails() throws Exception {
        mockMvc.perform(get("/api/reports").param("status", "FOO"))
                .andExpect(jsonPath("$.message").value(not(containsString("com.cityfix"))))
                .andExpect(jsonPath("$.message").value(not(containsString("Exception"))));

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(jsonPath("$.message").value(not(containsString("com.cityfix"))))
                .andExpect(jsonPath("$.message").value(not(containsString("Exception"))))
                .andExpect(jsonPath("$.message").value(not(containsString("JSON parse error"))));
    }
}
