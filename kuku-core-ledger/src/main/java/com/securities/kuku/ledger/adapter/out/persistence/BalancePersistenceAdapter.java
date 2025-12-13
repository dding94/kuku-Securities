package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.BalanceJpaEntity;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.domain.Balance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BalancePersistenceAdapter implements BalancePort {

    private final BalanceJpaRepository balanceJpaRepository;

    @Override
    public Optional<Balance> findByAccountId(Long accountId) {
        return balanceJpaRepository.findById(accountId)
                .map(BalanceJpaEntity::toDomain);
    }

    @Override
    public Map<Long, Balance> findByAccountIds(Set<Long> accountIds) {
        return balanceJpaRepository.findByAccountIdIn(accountIds)
                .stream()
                .map(BalanceJpaEntity::toDomain)
                .collect(Collectors.toMap(Balance::getAccountId, Function.identity()));
    }

    @Override
    public void update(Balance balance) {
        BalanceJpaEntity entity = balanceJpaRepository.findById(balance.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Balance not found: " + balance.getAccountId()));
        entity.updateFrom(balance);
        balanceJpaRepository.save(entity);
    }

    @Override
    public void updateAll(Collection<Balance> balances) {
        if (balances.isEmpty()) {
            return;
        }

        Set<Long> accountIds = balances.stream()
                .map(Balance::getAccountId)
                .collect(Collectors.toSet());

        Map<Long, BalanceJpaEntity> entityMap = balanceJpaRepository.findByAccountIdIn(accountIds)
                .stream()
                .collect(Collectors.toMap(BalanceJpaEntity::getAccountId, Function.identity()));

        balances.forEach(balance -> {
            BalanceJpaEntity entity = entityMap.get(balance.getAccountId());
            if (entity == null) {
                throw new IllegalArgumentException("Balance not found: " + balance.getAccountId());
            }
            entity.updateFrom(balance);
        });

        balanceJpaRepository.saveAll(entityMap.values());
    }
}
