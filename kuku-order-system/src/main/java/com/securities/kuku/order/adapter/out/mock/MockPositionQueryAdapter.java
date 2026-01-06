package com.securities.kuku.order.adapter.out.mock;

import com.securities.kuku.order.application.port.out.PositionQueryPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Week 5용 Mock 구현체. Week 9에서 Position 모듈 연동 시 교체 예정.
 *
 * <p>기본적으로 충분한 보유 수량(100주)을 반환합니다.
 */
@Component
public class MockPositionQueryAdapter implements PositionQueryPort {

  private static final BigDecimal DEFAULT_HOLDING_QUANTITY = new BigDecimal("100");

  @Override
  public BigDecimal getHoldingQuantity(Long accountId, String symbol) {
    return DEFAULT_HOLDING_QUANTITY;
  }
}
