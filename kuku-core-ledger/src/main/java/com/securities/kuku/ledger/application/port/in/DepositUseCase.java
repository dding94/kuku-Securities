package com.securities.kuku.ledger.application.port.in;

import com.securities.kuku.ledger.application.port.in.command.DepositCommand;

public interface DepositUseCase {
  void deposit(DepositCommand command);
}
