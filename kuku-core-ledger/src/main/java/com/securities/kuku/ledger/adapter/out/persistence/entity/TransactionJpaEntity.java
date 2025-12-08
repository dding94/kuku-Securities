package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionJpaEntity {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "description")
    private String description;

    @Column(name = "business_ref_id", unique = true)
    private String businessRefId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "reversal_of_transaction_id")
    private Long reversalOfTransactionId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionJpaEntity(Long id, TransactionType type, String description, String businessRefId,
            TransactionStatus status,
            Long reversalOfTransactionId, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.businessRefId = businessRefId;
        this.status = status;
        this.reversalOfTransactionId = reversalOfTransactionId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
