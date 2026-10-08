package com.example.delivery.auth.controller;

import com.example.delivery.auth.dto.LoginRequest;
import com.example.delivery.auth.dto.LoginResponse;
import com.example.delivery.auth.dto.LoginResult;
import com.example.delivery.auth.service.AuthService;
import com.example.delivery.global.security.JwtProvider;
import com.example.delivery.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(jwtProvider.getRefreshTokenExpirationSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("로그인에 성공했습니다.", new LoginResponse(result.accessToken())));
    }
}
