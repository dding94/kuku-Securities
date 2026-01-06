package com.securities.kuku.order.application.port.in;

import com.securities.kuku.order.application.port.in.command.PlaceOrderCommand;
import com.securities.kuku.order.domain.Order;

public interface PlaceOrderUseCase {
  Order placeOrder(PlaceOrderCommand command);
}
