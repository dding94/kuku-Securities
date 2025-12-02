package com.securities.kuku.ledger.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceJpaEntity {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "last_transaction_id")
    private Long lastTransactionId;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BalanceJpaEntity(Long accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
        this.updatedAt = LocalDateTime.now();
    }
}
