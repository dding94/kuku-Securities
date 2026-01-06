package com.securities.kuku.order.application.validation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class MarketHoursPolicy {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);
  private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);

  private final Clock clock;

  public MarketHoursPolicy(Clock clock) {
    this.clock = clock;
  }

  public boolean isMarketOpen(Instant instant) {
    ZonedDateTime koreaTime = instant.atZone(KOREA_ZONE);
    LocalTime localTime = koreaTime.toLocalTime();

    return !localTime.isBefore(MARKET_OPEN_TIME) && !localTime.isAfter(MARKET_CLOSE_TIME);
  }

  public boolean isMarketOpenNow() {
    return isMarketOpen(clock.instant());
  }
}
