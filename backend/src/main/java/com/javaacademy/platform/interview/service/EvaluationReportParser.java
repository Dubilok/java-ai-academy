package com.javaacademy.platform.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.interview.dto.EvaluationDimension;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EvaluationReportParser {

    static final int REQUIRED_DIMENSIONS = 10;

    private final ObjectMapper strictMapper;
    private final Validator validator;
    private final Clock clock;

    public EvaluationReportParser(ObjectMapper objectMapper, Validator validator, Clock clock) {
        this.strictMapper = objectMapper
                .copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        this.validator = validator;
        this.clock = clock;
    }

    public EvaluationReport parse(String llmJson, UUID sessionId, String technology) {
        List<EvaluationDimension> dimensions = parseDimensions(llmJson);
        validateDimensions(dimensions);

        int overallScore = (int) Math.round(dimensions.stream()
                .mapToInt(EvaluationDimension::score)
                .average()
                .orElse(0));

        EvaluationReport report =
                new EvaluationReport(sessionId, technology, overallScore, dimensions, Instant.now(clock));
        validateReport(report);
        return report;
    }

    private List<EvaluationDimension> parseDimensions(String llmJson) {
        try {
            JsonNode root = strictMapper.readTree(llmJson);
            JsonNode dimensionsNode = root.get("dimensions");
            if (dimensionsNode == null || !dimensionsNode.isArray()) {
                throw new LlmException("Evaluation JSON missing 'dimensions' array");
            }
            List<EvaluationDimension> dimensions = new ArrayList<>();
            for (JsonNode dimensionNode : dimensionsNode) {
                EvaluationDimension dimension = strictMapper.treeToValue(dimensionNode, EvaluationDimension.class);
                dimensions.add(dimension);
            }
            return dimensions;
        } catch (JsonProcessingException parseException) {
            throw new LlmException("Failed to parse evaluation JSON: " + parseException.getOriginalMessage());
        }
    }

    private void validateDimensions(List<EvaluationDimension> dimensions) {
        if (dimensions.size() != REQUIRED_DIMENSIONS) {
            throw new LlmException(
                    "Evaluation must have exactly " + REQUIRED_DIMENSIONS + " dimensions, got " + dimensions.size());
        }
        for (EvaluationDimension dimension : dimensions) {
            Set<ConstraintViolation<EvaluationDimension>> violations = validator.validate(dimension);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.joining("; "));
                throw new LlmException("Evaluation dimension '" + dimension.name() + "' failed validation: " + errors);
            }
        }
    }

    private void validateReport(EvaluationReport report) {
        Set<ConstraintViolation<EvaluationReport>> violations = validator.validate(report);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            throw new LlmException("Evaluation report failed validation: " + errors);
        }
    }
}
