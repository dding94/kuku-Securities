package com.securities.kuku.ledger.domain.exception;

import com.securities.kuku.common.exception.BusinessException;
import java.math.BigDecimal;
import lombok.Getter;

/** 잔액이 부족할 때 발생하는 예외. */
@Getter
public class InsufficientBalanceException extends BusinessException {

  private final Long accountId;
  private final BigDecimal requested;
  private final BigDecimal available;

  public InsufficientBalanceException(Long accountId, BigDecimal requested, BigDecimal available) {
    super(
        LedgerErrorCode.INSUFFICIENT_BALANCE,
        String.format(
            "Insufficient balance for account %d: requested=%s, available=%s",
            accountId, requested, available));
    this.accountId = accountId;
    this.requested = requested;
    this.available = available;
  }
}
