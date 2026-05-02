package com.gymtracker.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.azure-openai")
public class AzureOpenAiOnboardingProperties {

    private String endpoint;
    private String apiKey;
    private String deployment;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }
}

