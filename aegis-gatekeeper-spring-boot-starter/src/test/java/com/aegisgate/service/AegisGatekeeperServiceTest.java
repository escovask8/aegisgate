package com.aegisgate.service;

import com.aegisgate.autoconfigure.AegisGateProperties;
import com.aegisgate.model.DecisionContracts.DecisionRequest;
import com.aegisgate.model.DecisionContracts.DecisionResponse;
import com.aegisgate.model.DecisionContracts.GateEvaluationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AegisGatekeeperServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private BedrockExplainerClient bedrockExplainer;
    private AegisGateProperties properties;
    private AegisGatekeeperService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        bedrockExplainer = new LocalBedrockExplainer();
        properties = new AegisGateProperties();
        properties.getDecider().setServiceUrl("http://localhost:8000");
        service = new AegisGatekeeperService(restClientBuilder, bedrockExplainer, properties);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Fast-Path: Should auto-approve when confidence is high (>= 0.93)")
    void shouldAutoApproveWhenConfidenceHigh() throws Exception {
        var response = new DecisionResponse("ZERO_TOUCH_DEPLOY", 0.97, 112, "All clear");

        mockServer.expect(requestTo("http://localhost:8000/decide"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        GateEvaluationResult result = service.evaluateProductionGate("JUnit 100%, 0 CVEs");

        assertTrue(result.allowed());
        assertEquals("ZERO_TOUCH_DEPLOY", result.deciderMetrics().selectedOption());
        assertEquals(0.97, result.deciderMetrics().confidence());
        assertNull(result.detailedExplanation());
        mockServer.verify();
    }

    @Test
    @DisplayName("Smart-Fallback: Should block and produce Bedrock report when release is rejected")
    void shouldBlockAndExplainWhenRejected() throws Exception {
        var response = new DecisionResponse("BLOCK_PRODUCTION", 0.98, 120, "Critical CVE");

        mockServer.expect(requestTo("http://localhost:8000/decide"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        GateEvaluationResult result = service.evaluateProductionGate("CVE-2026-9999 critical vulnerability detected");

        assertFalse(result.allowed());
        assertEquals("BLOCK_PRODUCTION", result.deciderMetrics().selectedOption());
        assertNotNull(result.detailedExplanation());
        assertTrue(result.detailedExplanation().contains("Security Gate Violation"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Smart-Fallback: Should block when confidence is below threshold (< 0.93)")
    void shouldBlockWhenConfidenceBelowThreshold() throws Exception {
        var response = new DecisionResponse("ZERO_TOUCH_DEPLOY", 0.81, 115, "Inconclusive telemetry");

        mockServer.expect(requestTo("http://localhost:8000/decide"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        GateEvaluationResult result = service.evaluateProductionGate("Partial test coverage, unverified telemetry");

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Confidence threshold not met"));
        assertNotNull(result.detailedExplanation());
        mockServer.verify();
    }

    @Test
    @DisplayName("Fail-Closed: Should safely block release when Decider sidecar is down")
    void shouldFailClosedWhenDeciderIsDown() {
        mockServer.expect(requestTo("http://localhost:8000/decide"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        GateEvaluationResult result = service.evaluateProductionGate("Any context");

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("FAIL_CLOSED"));
        assertEquals("FAIL_CLOSED", result.deciderMetrics().selectedOption());
        mockServer.verify();
    }
}

