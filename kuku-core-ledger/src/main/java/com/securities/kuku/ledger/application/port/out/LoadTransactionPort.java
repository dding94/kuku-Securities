package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Transaction;
import java.util.Optional;

public interface LoadTransactionPort {
    Optional<Transaction> loadTransaction(String businessRefId);
}
