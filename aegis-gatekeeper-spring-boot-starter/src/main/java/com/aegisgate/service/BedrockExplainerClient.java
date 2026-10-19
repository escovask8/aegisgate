package com.aegisgate.service;

import com.aegisgate.model.DecisionContracts.DecisionResponse;

/**
 * Interface for generating deep risk explanations when a deployment gate fails or has low confidence.
 */
public interface BedrockExplainerClient {

    /**
     * Generates a concise 3-bullet point executive summary of failure causes and required remediation.
     *
     * @param metricsReport The raw metrics snapshot submitted to the gate
     * @param decision The outcome and confidence from Strands Decider 2B
     * @return Formatted explanation string
     */
    String generateExplanation(String metricsReport, DecisionResponse decision);
}

