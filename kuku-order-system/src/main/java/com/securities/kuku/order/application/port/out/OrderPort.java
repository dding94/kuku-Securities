package com.securities.kuku.order.application.port.out;

import com.securities.kuku.order.domain.Order;
import java.util.Optional;

public interface OrderPort {
  Order save(Order order);

  Optional<Order> findById(Long orderId);

  Order update(Order order);
}
