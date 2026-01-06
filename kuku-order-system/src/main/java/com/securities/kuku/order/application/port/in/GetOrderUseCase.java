package com.securities.kuku.order.application.port.in;

import com.securities.kuku.order.domain.Order;
import java.util.Optional;

public interface GetOrderUseCase {
  Optional<Order> getOrder(Long orderId);
}
