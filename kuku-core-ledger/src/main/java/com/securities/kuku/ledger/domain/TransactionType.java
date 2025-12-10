package com.securities.kuku.ledger.domain;

public enum TransactionType {
    DEPOSIT, // 입금
    WITHDRAWAL, // 출금
    REVERSAL, // 역분개
    ORDER_BLOCKED, // 주문 증거금 차단 (예수금/주식)
    ORDER_RELEASED, // 주문 미체결/취소로 인한 차단 해제
    TRADE, // 매매 체결 (자산 교환)
    FEE, // 수수료 차감
    INTEREST, // 이자 지급
    CORRECTION // 정정 (오류 수정 등)
}
