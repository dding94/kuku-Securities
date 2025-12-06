package com.securities.kuku.ledger.application.port.in;

import java.math.BigDecimal;

public interface WithdrawUseCase {
    void withdraw(Long accountId, BigDecimal amount, String description, String businessRefId);
}
