package com.securities.kuku.order.adapter.out.persistence;

import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.exception.OrderNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderPort {

  private final OrderJpaRepository orderJpaRepository;

  @Override
  public Order save(Order order) {
    OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
    OrderJpaEntity saved = orderJpaRepository.save(entity);
    return saved.toDomain();
  }

  @Override
  public Optional<Order> findById(Long orderId) {
    return orderJpaRepository.findById(orderId).map(OrderJpaEntity::toDomain);
  }

  @Override
  public Order update(Order order) {
    OrderJpaEntity entity =
        orderJpaRepository
            .findById(order.getId())
            .orElseThrow(() -> new OrderNotFoundException(order.getId()));

    entity.updateFrom(order);
    OrderJpaEntity saved = orderJpaRepository.save(entity);
    return saved.toDomain();
  }
}
