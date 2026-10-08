package com.example.delivery.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 회원
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "U001", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    DELETED_USER(HttpStatus.FORBIDDEN, "U003", "탈퇴한 회원입니다."),

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 HTTP 메서드입니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "C005", "이미 존재하거나 충돌하는 데이터입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
