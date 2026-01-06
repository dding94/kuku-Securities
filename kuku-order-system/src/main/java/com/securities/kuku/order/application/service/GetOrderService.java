package com.securities.kuku.order.application.service;

import com.securities.kuku.order.application.port.in.GetOrderUseCase;
import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderService implements GetOrderUseCase {

  private final OrderPort orderPort;

  @Override
  public Optional<Order> getOrder(Long orderId) {
    return orderPort.findById(orderId);
  }
}
