package com.javaacademy.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContentParser {

    private final ObjectMapper strictMapper;
    private final Validator validator;

    public ContentParser(ObjectMapper objectMapper, Validator validator) {
        this.strictMapper = objectMapper
                .copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        this.validator = validator;
    }

    public GeneratedContent parse(String json) {
        GeneratedContent content = parseJson(json);
        validateConstraints(content);
        validateTestCodeHasAnnotation(content.testCode());
        return content;
    }

    private GeneratedContent parseJson(String json) {
        try {
            return strictMapper.readValue(json, GeneratedContent.class);
        } catch (JsonProcessingException parseException) {
            throw new LlmException("Failed to parse AI-generated content: " + parseException.getOriginalMessage());
        }
    }

    private void validateConstraints(GeneratedContent content) {
        Set<ConstraintViolation<GeneratedContent>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            throw new LlmException("AI-generated content failed validation: " + errors);
        }
    }

    private static void validateTestCodeHasAnnotation(String testCode) {
        if (!testCode.contains("@Test")) {
            throw new LlmException("Generated test code must contain at least one @Test method");
        }
    }
}
