package com.securities.kuku.ledger.domain.exception;

import com.securities.kuku.common.exception.BusinessException;
import com.securities.kuku.ledger.domain.TransactionStatus;

/** 역분개가 불가능한 상태의 트랜잭션에 대해 역분개를 시도할 때 발생하는 예외. POSTED 상태가 아닌 트랜잭션(PENDING, REVERSED)은 역분개할 수 없다. */
public class InvalidTransactionStateException extends BusinessException {

  public InvalidTransactionStateException(String message) {
    super(LedgerErrorCode.INVALID_TRANSACTION_STATE, message);
  }

  public InvalidTransactionStateException(TransactionStatus currentStatus, String action) {
    super(
        LedgerErrorCode.INVALID_TRANSACTION_STATE,
        String.format("Cannot %s transaction in %s status", action, currentStatus));
  }
}
