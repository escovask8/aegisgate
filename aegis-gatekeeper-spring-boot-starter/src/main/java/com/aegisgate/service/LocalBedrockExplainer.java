package com.aegisgate.service;

import com.aegisgate.model.DecisionContracts.DecisionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Local simulation of Amazon Bedrock (Claude 3.5 Sonnet) risk assessment.
 * Allows deterministic local testing, offline development, and zero-cost unit testing.
 */
public class LocalBedrockExplainer implements BedrockExplainerClient {

    private static final Logger log = LoggerFactory.getLogger(LocalBedrockExplainer.class);

    @Override
    public String generateExplanation(String metricsReport, DecisionResponse decision) {
        log.info("Generating local Bedrock risk explanation for decision: {}", decision.selectedOption());

        String reportLower = metricsReport != null ? metricsReport.toLowerCase() : "";
        List<String> findings = new ArrayList<>();

        if (reportLower.contains("cve") || reportLower.contains("vulnerability")) {
            findings.add("Security Gate Violation: Critical or High CVEs detected in dependency hierarchy requiring immediate upgrade/patch.");
        }
        if (reportLower.contains("failed") || reportLower.contains("failure") || reportLower.contains("errors")) {
            findings.add("Quality Regression: Automated test suite reported failures or broken test executions.");
        }
        if (reportLower.contains("breaking") || reportLower.contains("incompatible") || reportLower.contains("migration")) {
            findings.add("Contract Incompatibility: Detected schema migration or OpenAPI breaking changes incompatible with current staging.");
        }
        if (reportLower.contains("sla") || reportLower.contains("latency") || reportLower.contains("breach")) {
            findings.add("Performance Degradation: Integration load testing breached acceptable p99 latency thresholds.");
        }

        if (findings.isEmpty()) {
            if (decision.confidence() < 0.90) {
                findings.add("Statistical Confidence Alert: Strands Decider 2B returned low calibrated confidence (" +
                        String.format("%.2f", decision.confidence()) + ") requiring human platform engineering review.");
                findings.add("Metrics Incompleteness: Input telemetry lacked required verification hashes or test coverage assertions.");
                findings.add("Deployment Precaution: Automatically gating candidate to prevent unverified drift into production.");
            } else {
                findings.add("Policy Enforcement: Release candidate flagged as " + decision.selectedOption() + " based on automated governance policies.");
                findings.add("Verification Required: Validate environment logs and staging verification telemetry before re-submitting.");
                findings.add("Action Item: Check build artifacts and run full pre-flight suite locally.");
            }
        }

        // Ensure exactly 3 concise bullets as required by spec
        while (findings.size() < 3) {
            findings.add("Action Item: Review build pipeline logs and resolve telemetry anomalies prior to promotion retry.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("AegisGate Deep Risk Analysis (Powered by Bedrock Claude 3.5 Sonnet [Local-Mode]):\n");
        sb.append("• ").append(findings.get(0)).append("\n");
        sb.append("• ").append(findings.get(1)).append("\n");
        sb.append("• ").append(findings.get(2));

        return sb.toString();
    }
}

