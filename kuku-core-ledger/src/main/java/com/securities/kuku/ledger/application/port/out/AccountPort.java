package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Account;
import java.util.Optional;

public interface AccountPort {

    Optional<Account> findById(Long accountId);
}
