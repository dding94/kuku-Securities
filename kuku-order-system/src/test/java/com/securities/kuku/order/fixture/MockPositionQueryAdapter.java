package com.securities.kuku.order.fixture;

import com.securities.kuku.order.application.port.out.PositionQueryPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 Mock 구현체. 기본적으로 충분한 보유 수량(100주)을 반환합니다.
 *
 * <p>특정 계좌/종목에 대해 커스텀 보유 수량을 설정할 수 있습니다.
 */
public class MockPositionQueryAdapter implements PositionQueryPort {

  private static final BigDecimal DEFAULT_HOLDING_QUANTITY = new BigDecimal("100");
  private final Map<String, BigDecimal> positions = new ConcurrentHashMap<>();

  @Override
  public BigDecimal getHoldingQuantity(Long accountId, String symbol) {
    String key = accountId + ":" + symbol;
    return positions.getOrDefault(key, DEFAULT_HOLDING_QUANTITY);
  }

  public void setPosition(Long accountId, String symbol, BigDecimal quantity) {
    String key = accountId + ":" + symbol;
    positions.put(key, quantity);
  }

  public void clear() {
    positions.clear();
  }
}
