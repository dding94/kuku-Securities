package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public TransactionJpaEntity(
      Long id,
      TransactionType type,
      String description,
      String businessRefId,
      TransactionStatus status,
      Long reversalOfTransactionId,
      Instant createdAt) {
    this.id = id;
    this.type = type;
    this.description = description;
    this.businessRefId = businessRefId;
    this.status = status;
    this.reversalOfTransactionId = reversalOfTransactionId;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
  }

  public Transaction toDomain() {
    return new Transaction(
        id, type, description, businessRefId, status, reversalOfTransactionId, createdAt);
  }

  public static TransactionJpaEntity fromDomain(Transaction transaction) {
    return new TransactionJpaEntity(
        transaction.getId(),
        transaction.getType(),
        transaction.getDescription(),
        transaction.getBusinessRefId(),
        transaction.getStatus(),
        transaction.getReversalOfTransactionId(),
        transaction.getCreatedAt());
  }

  public void updateStatus(TransactionStatus newStatus) {
    this.status = newStatus;
  }
}
