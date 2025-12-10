package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Transaction;
import java.util.Optional;

/**
 * Transaction Aggregate에 대한 통합 Outbound Port.
 * POLICY.md 가이드라인에 따라 동일 Aggregate의 CRUD를 하나의 Port로 통합.
 */
public interface TransactionPort {

    Optional<Transaction> findById(Long transactionId);

    Optional<Transaction> findByBusinessRefId(String businessRefId);

    Transaction save(Transaction transaction);

    void update(Transaction transaction);
}
