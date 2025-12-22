package com.securities.kuku.ledger.application.port.in.command;

import java.math.BigDecimal;

public record ConfirmTransactionCommand(
        Long transactionId,
        Long accountId,
        BigDecimal amount) {

    public ConfirmTransactionCommand {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
