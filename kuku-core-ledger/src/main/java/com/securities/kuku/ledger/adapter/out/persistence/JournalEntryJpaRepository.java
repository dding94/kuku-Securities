package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.JournalEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryJpaEntity, Long> {

    List<JournalEntryJpaEntity> findByTransactionId(Long transactionId);
}
