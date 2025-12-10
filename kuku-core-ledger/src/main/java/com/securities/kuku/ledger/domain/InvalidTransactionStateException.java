package com.securities.kuku.ledger.domain;

/**
 * 역분개가 불가능한 상태의 트랜잭션에 대해 역분개를 시도할 때 발생하는 예외.
 * POSTED 상태가 아닌 트랜잭션(PENDING, REVERSED)은 역분개할 수 없다.
 */
public class InvalidTransactionStateException extends RuntimeException {

    public InvalidTransactionStateException(String message) {
        super(message);
    }
}
