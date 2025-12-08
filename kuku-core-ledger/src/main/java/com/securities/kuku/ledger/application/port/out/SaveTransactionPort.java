package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Transaction;

public interface SaveTransactionPort {
    Transaction saveTransaction(Transaction transaction);
}
