package com.javaacademy.platform.catalog.util;

import com.javaacademy.platform.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class CursorEncoder {

    public record CursorParts(Instant createdAt, UUID id) {}

    public String encode(Instant createdAt, UUID id) {
        String raw = createdAt.toEpochMilli() + "~" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public CursorParts decode(String cursor) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split("~", 2);
            return new CursorParts(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }
}
