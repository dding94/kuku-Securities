package com.securities.kuku.ledger.application.port.in;

public interface ReversalUseCase {
    void reverse(Long originalTransactionId, String reason);
}
