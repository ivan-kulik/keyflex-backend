package com.keyflex.auth.dto.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean isSuccessful,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public ApiResponse {
        timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true, message, data, LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                false, message, null, LocalDateTime.now()
        );
    }
}
