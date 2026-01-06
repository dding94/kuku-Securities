package com.securities.kuku.order.application.port.in;

import com.securities.kuku.order.domain.Order;

public interface CancelOrderUseCase {
  Order cancelOrder(Long orderId);
}
