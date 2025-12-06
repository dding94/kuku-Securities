package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Account;
import java.util.Optional;

public interface LoadAccountPort {
    Optional<Account> loadAccount(Long accountId);

    Optional<Account> loadAccount(String accountNumber);
}
