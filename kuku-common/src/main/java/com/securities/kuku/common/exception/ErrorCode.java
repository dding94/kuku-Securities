package com.securities.kuku.common.exception;

/** 에러 코드 인터페이스. 각 도메인별 Enum이 구현합니다. */
public interface ErrorCode {

  String getCode();

  String getMessage();

  int getStatus();
}
