package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Balance;

public interface UpdateBalancePort {
    void updateBalance(Balance balance);
}
