package com.example.delivery.global.response;

import com.example.delivery.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<ValidationError> errors
) {
    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    // 성공
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, DEFAULT_SUCCESS_MESSAGE, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, null, message, data, null);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, null, message, null, null);
    }

    // 실패
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, List<ValidationError> errors) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, errors);
    }
}
