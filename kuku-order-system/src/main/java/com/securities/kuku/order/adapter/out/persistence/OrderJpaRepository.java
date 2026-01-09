package com.securities.kuku.order.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

  Optional<OrderJpaEntity> findByBusinessRefId(String businessRefId);
}
