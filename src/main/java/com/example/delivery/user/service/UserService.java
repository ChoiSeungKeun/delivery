package com.example.delivery.user.service;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.user.dto.UserJoinRequest;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void join(UserJoinRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        User user = User.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
    }
}
