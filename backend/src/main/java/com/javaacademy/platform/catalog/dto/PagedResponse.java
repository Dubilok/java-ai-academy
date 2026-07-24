package com.javaacademy.platform.catalog.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record PagedResponse<T>(List<T> items, @Nullable String nextCursor) {}
