package com.securities.kuku.ledger.application.port.in;

import java.math.BigDecimal;

public interface DepositUseCase {
    void deposit(Long accountId, BigDecimal amount, String description, String businessRefId);
}
