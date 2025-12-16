package cs209a.finalproject_demo.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        T data,
        Map<String, Object> meta,
        Instant timestamp
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Map.of(), Instant.now());
    }

    public static <T> ApiResponse<T> of(T data, Map<String, Object> meta) {
        return new ApiResponse<>(data, meta, Instant.now());
    }
}

























