package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.BalanceJpaEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceJpaRepository extends JpaRepository<BalanceJpaEntity, Long> {

  List<BalanceJpaEntity> findByAccountIdIn(Set<Long> accountIds);
}
