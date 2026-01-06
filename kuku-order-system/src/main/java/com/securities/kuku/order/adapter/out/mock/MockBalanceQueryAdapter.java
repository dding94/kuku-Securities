package com.securities.kuku.order.adapter.out.mock;

import com.securities.kuku.order.application.port.out.BalanceQueryPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Week 5용 Mock 구현체. Week 7에서 LedgerBalanceQueryAdapter로 교체 예정.
 *
 * <p>기본적으로 충분한 잔액(1,000,000원)을 반환합니다.
 */
@Component
public class MockBalanceQueryAdapter implements BalanceQueryPort {

  private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");

  @Override
  public Optional<BigDecimal> getAvailableBalance(Long accountId) {
    return Optional.of(DEFAULT_BALANCE);
  }
}
