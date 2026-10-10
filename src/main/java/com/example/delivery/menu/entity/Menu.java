package com.example.delivery.menu.entity;

import com.example.delivery.global.entity.SoftDeletableEntity;
import com.example.delivery.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "menu")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(length = 200)
    private String description;

    @Convert(converter = MenuTypeConverter.class)
    @Column(nullable = false)
    private MenuType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private Menu(User user, String name, int price, String description, MenuType type) {
        this.user = user;
        this.name = name;
        this.price = price;
        this.description = description;
        this.type = (type != null) ? type : MenuType.MAIN;
    }
}
