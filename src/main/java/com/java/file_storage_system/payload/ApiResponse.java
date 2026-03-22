package com.java.file_storage_system.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse <T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
    private String path;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, String path) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
        this.path = path;
    }


    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return new ApiResponse<>(true, message, data, path);
    }

    public static <T> ApiResponse<T> success(String message, String path) {
        return new ApiResponse<>(true, message, null, path);
    }

    public static <T> ApiResponse<T> error(String message, String path) {
        return new ApiResponse<>(false, message, null, path);
    }

    public static <T> ApiResponse<T> error(String message, T data, String path) {
        return new ApiResponse<>(false, message, data, path);
    }
}
