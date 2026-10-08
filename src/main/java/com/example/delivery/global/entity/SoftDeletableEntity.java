package com.example.delivery.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column
    private LocalDateTime deletedAt;

    @Column
    private Long deletedBy;

    public void delete(Long deletedBy) {
        if(isDeleted()) {
            return;
        }
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

}
