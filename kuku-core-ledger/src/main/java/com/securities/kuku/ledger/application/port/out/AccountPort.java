package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.Account;
import java.util.Optional;

/**
 * Account Aggregate에 대한 Outbound Port.
 */
public interface AccountPort {

    Optional<Account> findById(Long accountId);
}
