package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.BalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface BalanceJpaRepository extends JpaRepository<BalanceJpaEntity, Long> {

    List<BalanceJpaEntity> findByAccountIdIn(Set<Long> accountIds);
}
