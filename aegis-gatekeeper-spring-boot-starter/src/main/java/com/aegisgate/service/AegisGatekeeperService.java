package com.aegisgate.service;

import com.aegisgate.autoconfigure.AegisGateProperties;
import com.aegisgate.model.DecisionContracts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Core Quality Gate Service: coordinates Strands Decider 2B fast-path evaluation
 * with Amazon Bedrock deep risk analysis fallback under a strict Fail-Closed policy.
 */
public class AegisGatekeeperService {

    private static final Logger log = LoggerFactory.getLogger(AegisGatekeeperService.class);

    private final RestClient deciderClient;
    private final BedrockExplainerClient bedrockExplainer;
    private final AegisGateProperties properties;

    public AegisGatekeeperService(RestClient.Builder restClientBuilder,
                                  BedrockExplainerClient bedrockExplainer,
                                  AegisGateProperties properties) {
        String baseUrl = properties.getDecider().getServiceUrl();
        this.deciderClient = restClientBuilder.baseUrl(baseUrl).build();
        this.bedrockExplainer = bedrockExplainer;
        this.properties = properties;
        log.info("Initialized AegisGatekeeperService with Decider at: {}", baseUrl);
    }

    /**
     * Evaluates Gate 2: UAT -> Production.
     * Fast-path approved if ZERO_TOUCH_DEPLOY selected with confidence >= threshold (0.93).
     */
    public GateEvaluationResult evaluateProductionGate(String metricsReport) {
        String question = "Does this release candidate satisfy zero critical CVEs, pass all integration SLA requirements, and conform to production deployment safety windows?";
        List<String> options = List.of("ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION");
        double threshold = properties.getThreshold().getProduction();

        return evaluateGate("PRODUCTION", metricsReport, question, options, "ZERO_TOUCH_DEPLOY", threshold);
    }

    /**
     * Evaluates Gate 1: Dev -> UAT.
     * Fast-path approved if PROMOTE_UAT selected with confidence >= threshold (0.85).
     */
    public GateEvaluationResult evaluateUatGate(String metricsReport) {
        String question = "Based on test success rates, static analysis, and database schema compatibility, is this build stable for UAT environment promotion without introducing breaking regressions?";
        List<String> options = List.of("PROMOTE_UAT", "REJECT_DEV");
        double threshold = properties.getThreshold().getUat();

        return evaluateGate("UAT", metricsReport, question, options, "PROMOTE_UAT", threshold);
    }

    /**
     * Generalized gate evaluation logic.
     */
    public GateEvaluationResult evaluateGate(String gateName,
                                            String metricsReport,
                                            String question,
                                            List<String> options,
                                            String passOption,
                                            double confidenceThreshold) {
        DecisionRequest request = new DecisionRequest(metricsReport, question, options);
        DecisionResponse decision;

        // 1. Invoke Strands Decider 2B Sidecar
        try {
            log.info("Dispatching gate [{}] request to Strands Decider 2B", gateName);
            decision = deciderClient.post()
                    .uri("/decide")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DecisionResponse.class);

            if (decision == null) {
                return failClosed("Strands Decider returned empty response");
            }
        } catch (RestClientException ex) {
            log.error("Decider sidecar communication failure: {}. Activating FAIL_CLOSED policy.", ex.getMessage());
            return failClosed("Decider engine communication error: " + ex.getMessage());
        }

        log.info("Decider result for [{}]: option={}, confidence={}, latency={}ms",
                gateName, decision.selectedOption(), decision.confidence(), decision.latencyMs());

        // 2. Fast-Path Check
        boolean isApprovedOption = passOption.equals(decision.selectedOption());
        boolean meetsConfidence = decision.confidence() >= confidenceThreshold;

        if (isApprovedOption && meetsConfidence) {
            String reason = String.format("Auto-approved via high-confidence policy decision (Option: %s, Confidence: %.2f >= %.2f in %dms).",
                    decision.selectedOption(), decision.confidence(), confidenceThreshold, decision.latencyMs());
            return new GateEvaluationResult(true, reason, decision, null);
        }

        // 3. Smart-Fallback: Deep Risk Analysis via Amazon Bedrock (Claude 3.5 Sonnet)
        log.warn("Gate [{}] blocked or inconclusive. Triggering Amazon Bedrock deep risk synthesis...", gateName);
        String explanation = bedrockExplainer.generateExplanation(metricsReport, decision);

        String failureReason = isApprovedOption
                ? String.format("Confidence threshold not met (%.2f < %.2f). Gated for technical analysis.", decision.confidence(), confidenceThreshold)
                : String.format("Release blocked by policy decision: %s (Confidence: %.2f).", decision.selectedOption(), decision.confidence());

        return new GateEvaluationResult(false, failureReason, decision, explanation);
    }

    private GateEvaluationResult failClosed(String reason) {
        DecisionResponse emptyDecision = new DecisionResponse("FAIL_CLOSED", 0.0, 0, "Security default fail-closed triggered");
        String message = "FAIL_CLOSED: Gate blocked release automatically due to infrastructure safety policy. Cause: " + reason;
        return new GateEvaluationResult(false, message, emptyDecision, "FAIL_CLOSED triggered: Review platform sidecar availability.");
    }
}

