package com.aegisgate.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Data contracts for AegisGate Decision Engine, matching Strands Decider 2B API specifications.
 */
public class DecisionContracts {

    public record DecisionRequest(
        String context,
        String question,
        List<String> options
    ) {}

    public record DecisionResponse(
        String selectedOption,
        double confidence,
        long latencyMs,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String rationale
    ) {}

    public record GateEvaluationResult(
        boolean allowed,
        String reason,
        DecisionResponse deciderMetrics,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String detailedExplanation
    ) {
        public GateEvaluationResult(boolean allowed, String reason, DecisionResponse deciderMetrics) {
            this(allowed, reason, deciderMetrics, null);
        }
    }
}

