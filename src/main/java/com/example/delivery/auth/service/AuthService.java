package com.example.delivery.auth.service;

import com.example.delivery.auth.dto.LoginRequest;
import com.example.delivery.auth.dto.LoginResult;
import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.security.JwtProvider;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResult login(LoginRequest request) {
        // 아이디 존재 여부가 노출되지 않도록 아이디/비밀번호 오류는 동일한 예외를 던진다.
        User user = userRepository.findByLoginId(request.loginId())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 탈퇴 여부는 본인 확인(비밀번호 일치) 이후에만 알려준다.
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.DELETED_USER);
        }

        return new LoginResult(
                jwtProvider.createAccessToken(user.getLoginId(), user.getRole()),
                jwtProvider.createRefreshToken(user.getLoginId(), user.getRole())
        );
    }
}
