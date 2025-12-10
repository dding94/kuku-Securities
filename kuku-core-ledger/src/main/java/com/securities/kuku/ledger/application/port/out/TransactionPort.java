package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Transaction;
import java.util.Optional;

public interface TransactionPort {

    Optional<Transaction> findById(Long transactionId);

    Optional<Transaction> findByBusinessRefId(String businessRefId);

    Transaction save(Transaction transaction);

    void update(Transaction transaction);
}
