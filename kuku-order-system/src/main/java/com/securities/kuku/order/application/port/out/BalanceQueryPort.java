package com.securities.kuku.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface BalanceQueryPort {
  Optional<BigDecimal> getAvailableBalance(Long accountId);
}
