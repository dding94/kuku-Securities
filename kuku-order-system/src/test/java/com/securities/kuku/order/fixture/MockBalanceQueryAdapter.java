package com.securities.kuku.order.fixture;

import com.securities.kuku.order.application.port.out.BalanceQueryPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 Mock 구현체. 기본적으로 충분한 잔액(1,000,000원)을 반환합니다.
 *
 * <p>특정 계좌에 대해 커스텀 잔액을 설정할 수 있습니다.
 */
public class MockBalanceQueryAdapter implements BalanceQueryPort {

  private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");
  private final Map<Long, BigDecimal> balances = new ConcurrentHashMap<>();

  @Override
  public Optional<BigDecimal> getAvailableBalance(Long accountId) {
    return Optional.of(balances.getOrDefault(accountId, DEFAULT_BALANCE));
  }

  public void setBalance(Long accountId, BigDecimal balance) {
    balances.put(accountId, balance);
  }

  public void clear() {
    balances.clear();
  }
}
