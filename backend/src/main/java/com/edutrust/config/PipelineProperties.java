package com.edutrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edutrust.pipeline")
public class PipelineProperties {
    private String defaultVersion = "v3";

    public String getDefaultVersion() { return defaultVersion; }
    public void setDefaultVersion(String defaultVersion) { this.defaultVersion = defaultVersion; }
}
