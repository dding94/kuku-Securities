package com.securities.kuku.ledger.application.port.in;

import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;

public interface WithdrawUseCase {
  void withdraw(WithdrawCommand command);
}
