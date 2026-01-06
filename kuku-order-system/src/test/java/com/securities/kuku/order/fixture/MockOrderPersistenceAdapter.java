package com.securities.kuku.order.fixture;

import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트용 In-Memory Mock 구현체.
 *
 * <p>AtomicLong으로 ID를 생성하고, ConcurrentHashMap으로 주문을 저장합니다.
 */
public class MockOrderPersistenceAdapter implements OrderPort {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final Map<Long, Order> orders = new ConcurrentHashMap<>();

  @Override
  public Order save(Order order) {
    Long id = order.getId() != null ? order.getId() : idGenerator.getAndIncrement();
    Order savedOrder =
        new Order(
            id,
            order.getAccountId(),
            order.getSymbol(),
            order.getQuantity(),
            order.getSide(),
            order.getOrderType(),
            order.getPrice(),
            order.getStatus(),
            order.getRejectionReason(),
            order.getBusinessRefId(),
            order.getExecutedPrice(),
            order.getExecutedQuantity(),
            order.getCreatedAt(),
            order.getUpdatedAt());
    orders.put(id, savedOrder);
    return savedOrder;
  }

  @Override
  public Optional<Order> findById(Long orderId) {
    return Optional.ofNullable(orders.get(orderId));
  }

  @Override
  public Order update(Order order) {
    if (order.getId() == null || !orders.containsKey(order.getId())) {
      throw new IllegalArgumentException("Cannot update non-existent order");
    }
    orders.put(order.getId(), order);
    return order;
  }

  public void clear() {
    orders.clear();
    idGenerator.set(1L);
  }

  public int size() {
    return orders.size();
  }
}
