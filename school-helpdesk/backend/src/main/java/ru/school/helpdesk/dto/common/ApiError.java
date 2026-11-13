package ru.school.helpdesk.dto.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Единый формат ошибки REST API. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<Map<String, String>> violations) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path,
                              List<Map<String, String>> violations) {
        return new ApiError(Instant.now(), status, error, message, path, violations);
    }
}


