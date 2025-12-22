package com.securities.kuku.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Balance {
  private final Long accountId;
  private final BigDecimal amount;
  private final BigDecimal holdAmount;
  private final Long version;
  private final Long lastTransactionId;
  private final Instant updatedAt;

  public Balance(
      Long accountId,
      BigDecimal amount,
      BigDecimal holdAmount,
      Long version,
      Long lastTransactionId,
      Instant updatedAt) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account ID cannot be null");
    }
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }
    if (holdAmount == null || holdAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("HoldAmount cannot be null or negative");
    }
    if (updatedAt == null) {
      throw new IllegalArgumentException("UpdatedAt cannot be null");
    }
    this.accountId = accountId;
    this.amount = amount;
    this.holdAmount = holdAmount;
    this.version = version;
    this.lastTransactionId = lastTransactionId;
    this.updatedAt = updatedAt;
  }

  public BigDecimal getAvailableAmount() {
    return amount.subtract(holdAmount);
  }

  public Balance deposit(BigDecimal depositAmount, Long transactionId, Instant now) {
    if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Deposit amount must be positive");
    }
    if (now == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
    return new Balance(
        this.accountId,
        this.amount.add(depositAmount),
        this.holdAmount,
        this.version,
        transactionId,
        now);
  }

  public Balance withdraw(BigDecimal withdrawAmount, Long transactionId, Instant now) {
    if (withdrawAmount == null || withdrawAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Withdraw amount must be positive");
    }
    if (now == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
    BigDecimal available = getAvailableAmount();
    if (withdrawAmount.compareTo(available) > 0) {
      throw new InsufficientBalanceException(
          "Insufficient balance. Available: " + available + ", Requested: " + withdrawAmount);
    }
    return new Balance(
        this.accountId,
        this.amount.subtract(withdrawAmount),
        this.holdAmount,
        this.version,
        transactionId,
        now);
  }
}
