package com.securities.kuku.order.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MarketHoursPolicy")
class MarketHoursPolicyTest {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDate TRADING_DAY = LocalDate.of(2026, 1, 6); // 화요일

  private MarketHoursPolicy createPolicyWithFixedTime(LocalTime time) {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(TRADING_DAY, time, KOREA_ZONE);
    Clock fixedClock = Clock.fixed(zonedDateTime.toInstant(), KOREA_ZONE);
    return new MarketHoursPolicy(fixedClock);
  }

  private Instant toInstant(LocalTime time) {
    return ZonedDateTime.of(TRADING_DAY, time, KOREA_ZONE).toInstant();
  }

  @Nested
  @DisplayName("isMarketOpen")
  class IsMarketOpen {

    @Test
    @DisplayName("장 시작 전(08:59)에는 false 반환")
    void returnsFalse_beforeMarketOpen() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(8, 59));
      Instant instant = toInstant(LocalTime.of(8, 59));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정확히 장 시작 시간(09:00)에는 true 반환")
    void returnsTrue_atMarketOpen() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(9, 0));
      Instant instant = toInstant(LocalTime.of(9, 0));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("장 운영 중(10:00)에는 true 반환")
    void returnsTrue_duringMarketHours() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(10, 0));
      Instant instant = toInstant(LocalTime.of(10, 0));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정확히 장 마감 시간(15:30)에는 true 반환 (inclusive)")
    void returnsTrue_atMarketClose() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(15, 30));
      Instant instant = toInstant(LocalTime.of(15, 30));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("장 마감 후(15:31)에는 false 반환")
    void returnsFalse_afterMarketClose() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(15, 31));
      Instant instant = toInstant(LocalTime.of(15, 31));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("야간 시간(18:00)에는 false 반환")
    void returnsFalse_atEvening() {
      // Given
      MarketHoursPolicy policy = createPolicyWithFixedTime(LocalTime.of(18, 0));
      Instant instant = toInstant(LocalTime.of(18, 0));

      // When
      boolean result = policy.isMarketOpen(instant);

      // Then
      assertThat(result).isFalse();
    }
  }
}
