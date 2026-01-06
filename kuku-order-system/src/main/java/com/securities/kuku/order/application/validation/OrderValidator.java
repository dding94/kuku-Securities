package com.securities.kuku.order.application.validation;

import com.securities.kuku.order.application.port.out.BalanceQueryPort;
import com.securities.kuku.order.application.port.out.PositionQueryPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.RejectionReason;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

  private final BalanceQueryPort balanceQueryPort;
  private final PositionQueryPort positionQueryPort;
  private final MarketHoursPolicy marketHoursPolicy;

  public OrderValidator(
      BalanceQueryPort balanceQueryPort,
      PositionQueryPort positionQueryPort,
      MarketHoursPolicy marketHoursPolicy) {
    this.balanceQueryPort = balanceQueryPort;
    this.positionQueryPort = positionQueryPort;
    this.marketHoursPolicy = marketHoursPolicy;
  }

  public Optional<RejectionReason> validate(Order order) {
    if (!marketHoursPolicy.isMarketOpen(order.getCreatedAt())) {
      return Optional.of(RejectionReason.MARKET_CLOSED);
    }

    return switch (order.getSide()) {
      case BUY -> validateBuyOrder(order);
      case SELL -> validateSellOrder(order);
    };
  }

  private Optional<RejectionReason> validateBuyOrder(Order order) {
    BigDecimal requiredAmount = calculateRequiredAmount(order);
    BigDecimal availableBalance =
        balanceQueryPort.getAvailableBalance(order.getAccountId()).orElse(BigDecimal.ZERO);

    if (availableBalance.compareTo(requiredAmount) < 0) {
      return Optional.of(RejectionReason.INSUFFICIENT_BALANCE);
    }

    return Optional.empty();
  }

  private Optional<RejectionReason> validateSellOrder(Order order) {
    BigDecimal holdingQuantity =
        positionQueryPort.getHoldingQuantity(order.getAccountId(), order.getSymbol());

    if (holdingQuantity.compareTo(order.getQuantity()) < 0) {
      return Optional.of(RejectionReason.INSUFFICIENT_QUANTITY);
    }

    return Optional.empty();
  }

  private BigDecimal calculateRequiredAmount(Order order) {
    if (order.getPrice() == null) {
      // 시장가 주문의 경우, 가격이 null일 수 있음 (Week 7에서 시세 조회 연동)
      return BigDecimal.ZERO;
    }
    return order.getQuantity().multiply(order.getPrice());
  }
}
