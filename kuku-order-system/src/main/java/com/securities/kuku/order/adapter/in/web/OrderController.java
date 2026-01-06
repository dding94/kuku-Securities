package com.securities.kuku.order.adapter.in.web;

import com.securities.kuku.order.adapter.in.web.dto.OrderResponse;
import com.securities.kuku.order.adapter.in.web.dto.PlaceOrderRequest;
import com.securities.kuku.order.application.port.in.CancelOrderUseCase;
import com.securities.kuku.order.application.port.in.GetOrderUseCase;
import com.securities.kuku.order.application.port.in.PlaceOrderUseCase;
import com.securities.kuku.order.application.port.in.command.PlaceOrderCommand;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderNotFoundException;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final PlaceOrderUseCase placeOrderUseCase;
  private final GetOrderUseCase getOrderUseCase;
  private final CancelOrderUseCase cancelOrderUseCase;

  @PostMapping
  public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
    PlaceOrderCommand command = toCommand(request);
    Order order = placeOrderUseCase.placeOrder(command);
    OrderResponse response = OrderResponse.from(order);

    HttpStatus status =
        order.getStatus().isSuccessful() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status).body(response);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
    Order order =
        getOrderUseCase.getOrder(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    return ResponseEntity.ok(OrderResponse.from(order));
  }

  @PostMapping("/{orderId}/cancel")
  public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
    Order cancelledOrder = cancelOrderUseCase.cancelOrder(orderId);
    return ResponseEntity.ok(OrderResponse.from(cancelledOrder));
  }

  private PlaceOrderCommand toCommand(PlaceOrderRequest request) {
    OrderSide side = parseOrderSide(request.side());
    OrderType orderType = parseOrderType(request.orderType());

    return PlaceOrderCommand.of(
        request.accountId(),
        request.symbol(),
        request.quantity(),
        side,
        orderType,
        request.price(),
        request.businessRefId());
  }

  private OrderSide parseOrderSide(String side) {
    try {
      return OrderSide.valueOf(side.toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Invalid order side: " + side);
    }
  }

  private OrderType parseOrderType(String orderType) {
    try {
      return OrderType.valueOf(orderType.toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Invalid order type: " + orderType);
    }
  }
}
