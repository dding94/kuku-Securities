package com.securities.kuku.ledger.domain;

import com.securities.kuku.ledger.domain.event.LedgerPostedEvent;
import com.securities.kuku.ledger.domain.event.LedgerReversedEvent;
import com.securities.kuku.ledger.domain.exception.InvalidTransactionStateException;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Transaction {
  private static final String REVERSAL_BUSINESS_REF_PREFIX = "reversal-";

  private final Long id;
  private final TransactionType type;
  private final String description;
  private final String businessRefId;
  private final TransactionStatus status;
  private final Long reversalOfTransactionId;
  private final Instant createdAt;

  public Transaction(
      Long id,
      TransactionType type,
      String description,
      String businessRefId,
      TransactionStatus status,
      Long reversalOfTransactionId,
      Instant createdAt) {

    if (type == null) {
      throw new IllegalArgumentException("Transaction Type cannot be null");
    }
    if (status == null) {
      throw new IllegalArgumentException("Transaction Status cannot be null");
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("CreatedAt cannot be null");
    }
    if (status == TransactionStatus.REVERSED && reversalOfTransactionId != null) {
      throw new IllegalArgumentException(
          "A REVERSED transaction cannot be a reversal of another transaction");
    }
    if (reversalOfTransactionId != null && status != TransactionStatus.POSTED) {
      throw new IllegalArgumentException("Reversal transaction must be in POSTED state");
    }
    this.id = id;
    this.type = type;
    this.description = description;
    this.businessRefId = businessRefId;
    this.status = status;
    this.reversalOfTransactionId = reversalOfTransactionId;
    this.createdAt = createdAt;
  }

  public static Transaction createDeposit(String description, String businessRefId, Instant now) {
    return new Transaction(
        null,
        TransactionType.DEPOSIT,
        description,
        businessRefId,
        TransactionStatus.POSTED,
        null,
        now);
  }

  public static Transaction createWithdraw(String description, String businessRefId, Instant now) {
    return new Transaction(
        null,
        TransactionType.WITHDRAWAL,
        description,
        businessRefId,
        TransactionStatus.POSTED,
        null,
        now);
  }

  public static Transaction createReversal(Long originalTransactionId, String reason, Instant now) {
    return new Transaction(
        null,
        TransactionType.REVERSAL,
        reason,
        REVERSAL_BUSINESS_REF_PREFIX + originalTransactionId,
        TransactionStatus.POSTED,
        originalTransactionId,
        now);
  }

  public Transaction toReversed() {
    validateCanBeReversed();
    return withStatus(TransactionStatus.REVERSED);
  }

  public Transaction confirm() {
    if (this.status != TransactionStatus.PENDING) {
      throw new InvalidTransactionStateException(
          "Only PENDING transactions can be confirmed. Current status: " + this.status);
    }
    return withStatus(TransactionStatus.POSTED);
  }

  public JournalEntry createJournalEntry(Long accountId, BigDecimal amount, Instant now) {
    return switch (this.type) {
      case DEPOSIT -> JournalEntry.createCredit(this.id, accountId, amount, now);
      case WITHDRAWAL -> JournalEntry.createDebit(this.id, accountId, amount, now);
      default ->
          throw new IllegalArgumentException(
              "Transaction type " + this.type + " cannot create journal entry via this flow");
    };
  }

  public Transaction markAsUnknown() {
    if (this.status != TransactionStatus.PENDING) {
      throw new InvalidTransactionStateException(
          "Only PENDING transactions can be marked as UNKNOWN. Current status: " + this.status);
    }
    return withStatus(TransactionStatus.UNKNOWN);
  }

  public Transaction resolveUnknown(TransactionStatus targetStatus) {
    if (this.status != TransactionStatus.UNKNOWN) {
      throw new InvalidTransactionStateException(
          "Only UNKNOWN transactions can be resolved. Current status: " + this.status);
    }
    if (!this.status.canTransitionTo(targetStatus)) {
      throw new InvalidTransactionStateException(
          "Cannot transition from UNKNOWN to " + targetStatus);
    }
    return withStatus(targetStatus);
  }

  public void validateCanBeReversed() {
    if (this.status == TransactionStatus.REVERSED) {
      throw new InvalidTransactionStateException("Transaction is already reversed: " + this.id);
    }
    if (this.status == TransactionStatus.PENDING) {
      throw new InvalidTransactionStateException(
          "Cannot reverse a PENDING transaction: " + this.id);
    }
    if (this.status == TransactionStatus.UNKNOWN) {
      throw new InvalidTransactionStateException(
          "Cannot reverse an UNKNOWN transaction. Resolve it first: " + this.id);
    }
  }

  public LedgerPostedEvent toPostedEvent(
      Long accountId, BigDecimal amount, TransactionType transactionType) {
    return LedgerPostedEvent.of(this.id, accountId, amount, transactionType, this.createdAt);
  }

  public LedgerReversedEvent toReversedEvent(Long originalTransactionId, String reason) {
    return LedgerReversedEvent.of(this.id, originalTransactionId, reason, this.createdAt);
  }

  private Transaction withStatus(TransactionStatus newStatus) {
    return new Transaction(
        this.id,
        this.type,
        this.description,
        this.businessRefId,
        newStatus,
        this.reversalOfTransactionId,
        this.createdAt);
  }
}
