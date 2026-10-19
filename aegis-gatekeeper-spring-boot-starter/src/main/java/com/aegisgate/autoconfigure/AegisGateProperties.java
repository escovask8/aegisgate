package com.aegisgate.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aegisgate")
public class AegisGateProperties {

    private DeciderProperties decider = new DeciderProperties();
    private BedrockProperties bedrock = new BedrockProperties();
    private GateThresholdProperties threshold = new GateThresholdProperties();

    public DeciderProperties getDecider() {
        return decider;
    }

    public void setDecider(DeciderProperties decider) {
        this.decider = decider;
    }

    public BedrockProperties getBedrock() {
        return bedrock;
    }

    public void setBedrock(BedrockProperties bedrock) {
        this.bedrock = bedrock;
    }

    public GateThresholdProperties getThreshold() {
        return threshold;
    }

    public void setThreshold(GateThresholdProperties threshold) {
        this.threshold = threshold;
    }

    public static class DeciderProperties {
        private String serviceUrl = "http://localhost:8000";
        private int timeoutMs = 2500;

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class BedrockProperties {
        private String mode = "local"; // "aws" or "local"
        private String modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0";
        private String region = "us-east-1";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public static class GateThresholdProperties {
        private double production = 0.93;
        private double uat = 0.85;

        public double getProduction() {
            return production;
        }

        public void setProduction(double production) {
            this.production = production;
        }

        public double getUat() {
            return uat;
        }

        public void setUat(double uat) {
            this.uat = uat;
        }
    }
}

