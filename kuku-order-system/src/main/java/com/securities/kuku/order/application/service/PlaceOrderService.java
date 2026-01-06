package com.securities.kuku.order.application.service;

import com.securities.kuku.order.application.port.in.PlaceOrderUseCase;
import com.securities.kuku.order.application.port.in.command.PlaceOrderCommand;
import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.application.validation.OrderValidator;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.RejectionReason;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {

  private final Clock clock;
  private final OrderPort orderPort;
  private final OrderValidator orderValidator;

  @Override
  public Order placeOrder(PlaceOrderCommand command) {
    Instant now = clock.instant();

    Order order =
        Order.create(
            command.accountId(),
            command.symbol(),
            command.quantity(),
            command.side(),
            command.orderType(),
            command.price(),
            command.businessRefId(),
            now);

    Optional<RejectionReason> rejectionReason = orderValidator.validate(order);

    Order finalOrder =
        rejectionReason
            .map(reason -> order.reject(reason, now))
            .orElseGet(() -> order.validate(now));

    return orderPort.save(finalOrder);
  }
}
