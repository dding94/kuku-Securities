package com.securities.kuku.order.application.service;

import com.securities.kuku.order.application.port.in.CancelOrderUseCase;
import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.exception.OrderNotFoundException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelOrderService implements CancelOrderUseCase {

  private final Clock clock;
  private final OrderPort orderPort;

  @Override
  public Order cancelOrder(Long orderId) {
    Order order =
        orderPort.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

    Instant now = clock.instant();
    Order cancelledOrder = order.cancel(now);

    return orderPort.update(cancelledOrder);
  }
}
