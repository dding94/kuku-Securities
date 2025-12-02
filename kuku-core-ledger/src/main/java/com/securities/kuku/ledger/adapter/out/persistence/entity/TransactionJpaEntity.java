package com.securities.kuku.ledger.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionJpaEntity {

    @Id
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "description")
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TransactionJpaEntity(Long id, String type, String description) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
