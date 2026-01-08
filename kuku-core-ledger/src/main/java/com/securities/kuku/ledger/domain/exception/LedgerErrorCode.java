package com.securities.kuku.ledger.domain.exception;

import com.securities.kuku.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Ledger 도메인 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum LedgerErrorCode implements ErrorCode {
  INSUFFICIENT_BALANCE("LEDGER_001", "Insufficient balance", 422),
  INVALID_TRANSACTION_STATE("LEDGER_002", "Invalid transaction state", 409),
  ACCOUNT_NOT_FOUND("LEDGER_003", "Account not found", 404),
  TRANSACTION_NOT_FOUND("LEDGER_004", "Transaction not found", 404),
  BALANCE_NOT_FOUND("LEDGER_005", "Balance not found", 404),
  DUPLICATE_TRANSACTION("LEDGER_006", "Duplicate transaction", 409);

  private final String code;
  private final String message;
  private final int status;
}
