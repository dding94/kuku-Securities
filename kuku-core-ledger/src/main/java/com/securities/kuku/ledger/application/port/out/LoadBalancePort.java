package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Balance;
import java.util.Optional;

public interface LoadBalancePort {
    Optional<Balance> loadBalance(Long accountId);
}
