package com.example.delivery.user.dto;

import com.example.delivery.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserJoinRequest(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(min = 4, max = 20, message = "로그인 ID는 4~20자여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotNull(message = "회원 역할은 필수입니다.")
        UserRole role
) {
}
