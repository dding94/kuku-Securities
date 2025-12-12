package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountJpaEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AccountType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AccountJpaEntity(Long id, Long userId, String accountNumber, String currency,
            AccountType type, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.type = type;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Account toDomain() {
        return new Account(id, userId, accountNumber, currency, type, createdAt);
    }

    public static AccountJpaEntity fromDomain(Account account) {
        return new AccountJpaEntity(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getType(),
                account.getCreatedAt());
    }
}
