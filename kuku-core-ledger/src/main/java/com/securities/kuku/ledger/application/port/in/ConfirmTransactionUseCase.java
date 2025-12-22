package com.securities.kuku.ledger.application.port.in;

import com.securities.kuku.ledger.application.port.in.command.ConfirmTransactionCommand;

public interface ConfirmTransactionUseCase {
    void confirm(ConfirmTransactionCommand command);
}
