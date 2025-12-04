package com.securities.kuku.ledger.domain;

public enum AccountType {
    USER_CASH,          // 사용자 예수금 계좌
    USER_SECURITIES,    // 사용자 증권(주식) 계좌
    SYSTEM_FEE,         // 시스템 수수료 수익 계좌
    SYSTEM_PNL,         // 시스템 손익(Profit & Loss) 계좌 (내부 운용 등)
    EXCHANGE_CLEARING   // 거래소 청산 계좌 (외부 연동용)
}
