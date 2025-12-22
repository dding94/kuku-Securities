package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.Balance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  @Column(name = "hold_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal holdAmount;

  @Version
  @Column(name = "version")
  private Long version;

  @Column(name = "last_transaction_id")
  private Long lastTransactionId;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public BalanceJpaEntity(
      Long accountId,
      BigDecimal amount,
      BigDecimal holdAmount,
      Long version,
      Long lastTransactionId,
      Instant updatedAt) {
    this.accountId = accountId;
    this.amount = amount;
    this.holdAmount = holdAmount != null ? holdAmount : BigDecimal.ZERO;
    this.version = version;
    this.lastTransactionId = lastTransactionId;
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public Balance toDomain() {
    return new Balance(accountId, amount, holdAmount, version, lastTransactionId, updatedAt);
  }

  public static BalanceJpaEntity fromDomain(Balance balance) {
    return new BalanceJpaEntity(
        balance.getAccountId(),
        balance.getAmount(),
        balance.getHoldAmount(),
        balance.getVersion(),
        balance.getLastTransactionId(),
        balance.getUpdatedAt());
  }

  public void updateFrom(Balance balance) {
    this.amount = balance.getAmount();
    this.holdAmount = balance.getHoldAmount();
    this.lastTransactionId = balance.getLastTransactionId();
    this.updatedAt = balance.getUpdatedAt();
  }
}
