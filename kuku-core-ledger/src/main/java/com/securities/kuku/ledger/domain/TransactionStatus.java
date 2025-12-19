package com.securities.kuku.ledger.domain;

public enum TransactionStatus {
    PENDING,
    POSTED,
    REVERSED,
    UNKNOWN;

    public boolean isConfirmed() {
        return this == POSTED;
    }

    public boolean canBeReversed() {
        return this == POSTED;
    }

    public boolean canTransitionTo(TransactionStatus target) {
        return switch (this) {
            case PENDING -> target == POSTED || target == UNKNOWN;
            case UNKNOWN -> target == POSTED;
            case POSTED -> target == REVERSED;
            case REVERSED -> false;
        };
    }
}
