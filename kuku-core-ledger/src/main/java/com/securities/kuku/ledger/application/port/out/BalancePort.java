package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Balance;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BalancePort {

  Optional<Balance> findByAccountId(Long accountId);

  Map<Long, Balance> findByAccountIds(Set<Long> accountIds);

  void update(Balance balance);

  void updateAll(Collection<Balance> balances);
}
