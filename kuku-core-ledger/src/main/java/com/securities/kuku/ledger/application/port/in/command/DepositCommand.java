package com.securities.kuku.ledger.application.port.in.command;

import java.math.BigDecimal;

public record DepositCommand(
        Long accountId,
        BigDecimal amount,
        String description,
        String businessRefId) {

    public DepositCommand {
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

    public static DepositCommand of(Long accountId, BigDecimal amount, String description, String businessRefId) {
        return new DepositCommand(accountId, amount, description, businessRefId);
    }

    public static DepositCommand of(Long accountId, BigDecimal amount, String businessRefId) {
        return new DepositCommand(accountId, amount, null, businessRefId);
    }
}
