package com.securities.kuku.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PlaceOrderRequest(
    @NotNull(message = "accountId is required") Long accountId,
    @NotBlank(message = "symbol is required") String symbol,
    @NotNull(message = "quantity is required") @Positive(message = "quantity must be positive")
        BigDecimal quantity,
    @NotBlank(message = "side is required") String side,
    @NotBlank(message = "orderType is required") String orderType,
    BigDecimal price,
    String businessRefId) {}
