package com.edutrust.api;

import com.edutrust.config.InstitutionProperties;
import com.edutrust.config.PipelineProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final InstitutionProperties properties;
    private final PipelineProperties pipelineProperties;

    public ConfigController(InstitutionProperties properties, PipelineProperties pipelineProperties) {
        this.properties = properties;
        this.pipelineProperties = pipelineProperties;
    }

    @GetMapping
    public ConfigResponse config() {
        return new ConfigResponse(
                properties.getName(),
                properties.getDepartments().stream().map(InstitutionProperties.Department::getCode).toList(),
                properties.getDocumentTypes().stream().map(InstitutionProperties.DocumentType::getName).toList(),
                List.of("v1", "v2", "v3"),
                pipelineProperties.getDefaultVersion());
    }

    public record ConfigResponse(String institutionName, List<String> departments,
                                 List<String> documentTypes, List<String> versions,
                                 String defaultVersion) {}
}
