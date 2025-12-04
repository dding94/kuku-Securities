package com.securities.kuku.ledger.domain;

/**
 * 트랜잭션의 생명주기 상태를 나타낸다.
 *
 * <ul>
 * <li>PENDING - 트랜잭션이 생성되었으나 아직 확정되지 않음</li>
 * <li>POSTED - 트랜잭션이 확정되어 잔액에 반영됨</li>
 * <li>REVERSED - 역분개되어 무효화됨</li>
 * </ul>
 */
public enum TransactionStatus {
    PENDING,
    POSTED,
    REVERSED;

    /**
     * 트랜잭션이 확정되어 잔액에 반영된 상태인지 확인한다.
     */
    public boolean isConfirmed() {
        return this == POSTED;
    }

    /**
     * 역분개가 가능한 상태인지 확인한다.
     * POSTED 상태의 트랜잭션만 역분개할 수 있다.
     */
    public boolean canBeReversed() {
        return this == POSTED;
    }
}
