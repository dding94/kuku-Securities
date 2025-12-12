package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    Optional<TransactionJpaEntity> findByBusinessRefId(String businessRefId);
}
