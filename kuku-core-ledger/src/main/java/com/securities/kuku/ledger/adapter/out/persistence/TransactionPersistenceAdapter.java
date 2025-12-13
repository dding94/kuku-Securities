package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.TransactionJpaEntity;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionPort {

    private final TransactionJpaRepository transactionJpaRepository;

    @Override
    public Optional<Transaction> findById(Long transactionId) {
        return transactionJpaRepository.findById(transactionId)
                .map(TransactionJpaEntity::toDomain);
    }

    @Override
    public Optional<Transaction> findByBusinessRefId(String businessRefId) {
        return transactionJpaRepository.findByBusinessRefId(businessRefId)
                .map(TransactionJpaEntity::toDomain);
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = TransactionJpaEntity.fromDomain(transaction);
        TransactionJpaEntity saved = transactionJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void update(Transaction transaction) {
        TransactionJpaEntity entity = transactionJpaRepository.findById(transaction.getId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transaction.getId()));
        entity.updateStatus(transaction.getStatus());
        transactionJpaRepository.save(entity);
    }
}
