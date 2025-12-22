package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.JournalEntryJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryJpaEntity, Long> {

  List<JournalEntryJpaEntity> findByTransactionId(Long transactionId);
}
