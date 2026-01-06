package com.securities.kuku.order.application.port.out;

import java.math.BigDecimal;

public interface PositionQueryPort {
  BigDecimal getHoldingQuantity(Long accountId, String symbol);
}
