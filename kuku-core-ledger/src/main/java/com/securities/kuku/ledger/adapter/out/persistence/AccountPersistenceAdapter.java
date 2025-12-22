package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.AccountJpaEntity;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.domain.Account;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountPort {

  private final AccountJpaRepository accountJpaRepository;

  @Override
  public Optional<Account> findById(Long accountId) {
    return accountJpaRepository.findById(accountId).map(AccountJpaEntity::toDomain);
  }
}
