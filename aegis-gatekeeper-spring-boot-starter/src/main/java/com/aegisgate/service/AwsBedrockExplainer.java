package com.aegisgate.service;

import com.aegisgate.model.DecisionContracts.DecisionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Amazon Bedrock implementation invoking Anthropic Claude 3.5 Sonnet on AWS.
 */
public class AwsBedrockExplainer implements BedrockExplainerClient {

    private static final Logger log = LoggerFactory.getLogger(AwsBedrockExplainer.class);

    private final BedrockRuntimeClient bedrockClient;
    private final String modelId;
    private final ObjectMapper objectMapper;

    public AwsBedrockExplainer(BedrockRuntimeClient bedrockClient, String modelId) {
        this.bedrockClient = bedrockClient;
        this.modelId = (modelId != null && !modelId.isBlank()) ? modelId : "anthropic.claude-3-5-sonnet-20240620-v1:0";
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateExplanation(String metricsReport, DecisionResponse decision) {
        log.info("Invoking Amazon Bedrock model: {} for risk synthesis", modelId);

        String prompt = """
            Human: The deployment promotion gate was BLOCKED.
            Strands Decider Output: %s (Confidence: %.2f)
            Metrics Snapshot:
            %s
            
            Provide a concise 3-bullet point executive summary explaining the exact failure causes and required fixes for the engineering team. Do not add introductory conversational text.
            Assistant:
            """.formatted(decision.selectedOption(), decision.confidence(), metricsReport);

        try {
            var payloadNode = objectMapper.createObjectNode();
            payloadNode.put("anthropic_version", "bedrock-2023-05-31");
            payloadNode.put("max_tokens", 500);

            var messagesArray = payloadNode.putArray("messages");
            var userMessage = messagesArray.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String requestBody = objectMapper.writeValueAsString(payloadNode);

            InvokeModelResponse response = bedrockClient.invokeModel(InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build());

            String responseString = response.body().asUtf8String();
            JsonNode root = objectMapper.readTree(responseString);
            JsonNode contentArray = root.path("content");
            if (contentArray.isArray() && !contentArray.isEmpty()) {
                return contentArray.get(0).path("text").asText();
            }
            return responseString;
        } catch (Exception e) {
            log.error("Failed to invoke Amazon Bedrock: {}. Falling back to defensive report.", e.getMessage(), e);
            return "AegisGate Bedrock Fallback Alert: Unable to retrieve live Claude 3.5 analysis (" + e.getMessage() +
                    "). Promotion blocked under FAIL_CLOSED policy.";
        }
    }
}

