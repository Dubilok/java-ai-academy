package com.javaacademy.platform.sandbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sandbox")
public record SandboxProperties(String image, long memoryBytes, int timeoutSeconds, int maxConcurrent) {}
