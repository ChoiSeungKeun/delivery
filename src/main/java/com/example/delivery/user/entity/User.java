package com.example.delivery.user.entity;

import com.example.delivery.global.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Builder
    private User(String loginId, String password, UserRole role) {
        this.loginId = loginId;
        this.password = password;
        this.role = role;
    }

}
