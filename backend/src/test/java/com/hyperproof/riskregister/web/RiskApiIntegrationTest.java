package com.hyperproof.riskregister.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end API tests over the full stack (controllers -> services -> Flyway-migrated H2),
 * covering the happy path and the key failure cases. {@code @Transactional} rolls back each test
 * for isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RiskApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String riskJson(String title, String category, String status, int likelihood, int impact) {
        return """
            {"title":"%s","description":"d","category":"%s","owner":"o",
             "likelihood":%d,"impact":%d,"status":"%s"}
            """.formatted(title, category, likelihood, impact, status);
    }

    @Test
    void createAddMitigationFetch_residualDropsAndSeveritiesAreCorrect() throws Exception {
        // Create a Critical risk (5x5 = 25, no mitigations -> residual 25).
        String created = mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(riskJson("Breach", "SECURITY", "OPEN", 5, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inherentScore").value(25))
                .andExpect(jsonPath("$.inherentSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.residualScore").value(25))
                .andExpect(jsonPath("$.residualSeverity").value("CRITICAL"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // Add a highly effective mitigation -> residual should drop to Low (5).
        mvc.perform(post("/api/risks/" + id + "/mitigations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"description":"Patch + WAF","effectiveness":5}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.residualScore").value(5))
                .andExpect(jsonPath("$.residualSeverity").value("LOW"))
                .andExpect(jsonPath("$.mitigationCount").value(1));

        // Fetch and confirm the residual persisted through a fresh read.
        mvc.perform(get("/api/risks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residualScore").value(5))
                .andExpect(jsonPath("$.inherentScore").value(25));
    }

    @Test
    void invalidLikelihoodReturns400WithFieldError() throws Exception {
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(riskJson("Bad", "SECURITY", "OPEN", 9, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.likelihood").exists());
    }

    @Test
    void blankTitleReturns400() throws Exception {
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(riskJson("", "SECURITY", "OPEN", 3, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRiskReturns404() throws Exception {
        mvc.perform(get("/api/risks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void closingRiskWithoutMitigationReturns409() throws Exception {
        String created = mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(riskJson("ToClose", "OPERATIONAL", "OPEN", 2, 2)))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(put("/api/risks/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(riskJson("ToClose", "OPERATIONAL", "CLOSED", 2, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("mitigation")));
    }

    @Test
    void listFiltersByStatusAndSortsByResidualDescending() throws Exception {
        // A: high residual (5x5, no mitigation = 25). B: low residual (2x2 = 4).
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                .content(riskJson("HighA", "FINANCIAL", "OPEN", 5, 5)));
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                .content(riskJson("LowB", "FINANCIAL", "OPEN", 2, 2)));
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                .content(riskJson("ClosedC", "FINANCIAL", "MITIGATING", 4, 4)));

        mvc.perform(get("/api/risks")
                        .param("category", "FINANCIAL").param("status", "OPEN").param("sort", "residualDesc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", contains("HighA", "LowB")));
    }

    @Test
    void badEnumQueryParamReturns400() throws Exception {
        mvc.perform(get("/api/risks").param("category", "NOPE"))
                .andExpect(status().isBadRequest());
    }
}
