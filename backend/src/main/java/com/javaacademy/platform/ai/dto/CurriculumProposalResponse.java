package com.javaacademy.platform.ai.dto;

import java.util.List;

public record CurriculumProposalResponse(
        String technology, String courseName, String description, List<ModuleProposal> modules) {}
