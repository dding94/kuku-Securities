package com.securities.kuku.ledger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/** Spring Retry 활성화 설정. Optimistic Lock 충돌 시 자동 재시도를 가능하게 합니다. */
@Configuration
@EnableRetry
public class RetryConfig {}
