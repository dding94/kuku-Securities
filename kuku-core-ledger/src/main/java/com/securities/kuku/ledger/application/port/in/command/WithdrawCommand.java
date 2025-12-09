package com.securities.kuku.ledger.application.port.in.command;

import java.math.BigDecimal;

public record WithdrawCommand(
        Long accountId,
        BigDecimal amount,
        String description,
        String businessRefId) {

    public WithdrawCommand {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (businessRefId == null || businessRefId.isBlank()) {
            throw new IllegalArgumentException("Business Reference ID cannot be null or empty");
        }
    }

    public static WithdrawCommand of(Long accountId, BigDecimal amount, String description, String businessRefId) {
        return new WithdrawCommand(accountId, amount, description, businessRefId);
    }

    public static WithdrawCommand of(Long accountId, BigDecimal amount, String businessRefId) {
        return new WithdrawCommand(accountId, amount, null, businessRefId);
    }
}
