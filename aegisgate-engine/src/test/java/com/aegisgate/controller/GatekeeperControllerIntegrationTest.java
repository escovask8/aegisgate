package com.aegisgate.controller;

import com.aegisgate.model.DecisionContracts.DecisionResponse;
import com.aegisgate.model.DecisionContracts.GateEvaluationResult;
import com.aegisgate.service.AegisGatekeeperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatekeeperControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AegisGatekeeperService gatekeeperService;

    @Test
    @DisplayName("Health endpoint should return UP")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/gates/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Fast-Path: Should return HTTP 200 when release is auto-approved")
    void shouldReturn200WhenReleaseApproved() throws Exception {
        var decision = new DecisionResponse("ZERO_TOUCH_DEPLOY", 0.97, 114, "Compliant release");
        var result = new GateEvaluationResult(true, "Auto-approved via high-confidence policy decision.", decision, null);

        when(gatekeeperService.evaluateProductionGate(anyString())).thenReturn(result);

        String metricsSnapshot = "JUnit: 100% passed. CVEs: 0. SLAs: 80ms.";

        mockMvc.perform(post("/api/v1/gates/production/evaluate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(metricsSnapshot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.deciderMetrics.selectedOption").value("ZERO_TOUCH_DEPLOY"))
                .andExpect(jsonPath("$.deciderMetrics.confidence").value(0.97));
    }

    @Test
    @DisplayName("Smart-Fallback: Should return HTTP 422 with explanation when release is blocked")
    void shouldReturn422WhenReleaseBlocked() throws Exception {
        var decision = new DecisionResponse("BLOCK_PRODUCTION", 0.98, 118, "Critical CVE detected");
        var explanation = "• Security Gate: CVE-2026-1123 found.\n• SLA: OK.\n• Recommendation: Patch dependency.";
        var result = new GateEvaluationResult(false, "Release blocked by policy decision: BLOCK_PRODUCTION.", decision, explanation);

        when(gatekeeperService.evaluateProductionGate(anyString())).thenReturn(result);

        String metricsSnapshot = "Security scan: CVE-2026-1123 in spring-core.";

        mockMvc.perform(post("/api/v1/gates/production/evaluate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(metricsSnapshot))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.deciderMetrics.selectedOption").value("BLOCK_PRODUCTION"))
                .andExpect(jsonPath("$.detailedExplanation").isNotEmpty());
    }

    @Test
    @DisplayName("Dev -> UAT: Should return HTTP 200 when UAT promotion passes")
    void shouldReturn200WhenUatGatePasses() throws Exception {
        var decision = new DecisionResponse("PROMOTE_UAT", 0.92, 110, "Tests and contracts valid");
        var result = new GateEvaluationResult(true, "Auto-approved UAT promotion.", decision, null);

        when(gatekeeperService.evaluateUatGate(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/v1/gates/uat/evaluate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Tests: 50/50 passed."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.deciderMetrics.selectedOption").value("PROMOTE_UAT"));
    }
}

