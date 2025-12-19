package com.securities.kuku.ledger.domain;

public enum TransactionStatus {
    PENDING,
    POSTED,
    REVERSED,
    UNKNOWN;

    public boolean isConfirmed() {
        return switch (this) {
            case POSTED -> true;
            case PENDING, REVERSED, UNKNOWN -> false;
        };
    }

    public boolean canBeReversed() {
        return switch (this) {
            case POSTED -> true;
            case PENDING, REVERSED, UNKNOWN -> false;
        };
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
