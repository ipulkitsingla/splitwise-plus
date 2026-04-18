package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int statusCode;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).statusCode(200).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).statusCode(200).build();
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder().success(false).message(message).statusCode(statusCode).build();
    }
}

