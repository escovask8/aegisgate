package com.aegisgate.autoconfigure;

import com.aegisgate.service.AegisGatekeeperService;
import com.aegisgate.service.AwsBedrockExplainer;
import com.aegisgate.service.BedrockExplainerClient;
import com.aegisgate.service.LocalBedrockExplainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration")
@EnableConfigurationProperties(AegisGateProperties.class)
public class AegisGatekeeperAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AegisGatekeeperAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder(ObjectMapper objectMapper) {
        return RestClient.builder()
                .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter(objectMapper)));
    }

    @Bean
    @ConditionalOnMissingBean
    public BedrockExplainerClient bedrockExplainerClient(AegisGateProperties properties) {
        String mode = properties.getBedrock().getMode();

        if ("aws".equalsIgnoreCase(mode)) {
            try {
                String regionName = properties.getBedrock().getRegion();
                log.info("Initializing AWS Bedrock Runtime client in region: {}", regionName);
                BedrockRuntimeClient bedrockRuntimeClient = BedrockRuntimeClient.builder()
                        .region(Region.of(regionName))
                        .build();

                return new AwsBedrockExplainer(bedrockRuntimeClient, properties.getBedrock().getModelId());
            } catch (Exception ex) {
                log.warn("Unable to initialize AWS Bedrock Runtime Client ({}: {}). Defaulting to LocalBedrockExplainer.",
                        ex.getClass().getSimpleName(), ex.getMessage());
                return new LocalBedrockExplainer();
            }
        }

        log.info("Using LocalBedrockExplainer (deterministic offline mode for development and local testing).");
        return new LocalBedrockExplainer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AegisGatekeeperService aegisGatekeeperService(RestClient.Builder restClientBuilder,
                                                         BedrockExplainerClient bedrockExplainerClient,
                                                         AegisGateProperties properties) {
        return new AegisGatekeeperService(restClientBuilder, bedrockExplainerClient, properties);
    }
}

