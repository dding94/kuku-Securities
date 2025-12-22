package com.securities.kuku.ledger.application.port.in;

import com.securities.kuku.ledger.application.port.in.command.ReversalCommand;

public interface ReversalUseCase {
  void reverse(ReversalCommand command);
}
