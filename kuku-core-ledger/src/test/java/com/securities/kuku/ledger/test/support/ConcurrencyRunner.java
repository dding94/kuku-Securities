package com.securities.kuku.ledger.test.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 동시성 테스트를 위한 유틸리티 클래스. 복잡한 스레드 관리, Latch 제어, 예외 수집 로직을 캡슐화합니다. */
public class ConcurrencyRunner {

  private static final Logger log = LoggerFactory.getLogger(ConcurrencyRunner.class);
  private static final int LATCH_TIMEOUT_SECONDS = 30;

  /**
   * 작업을 지정된 스레드 수만큼 동시에 실행합니다.
   *
   * @param threadCount 동시 실행할 스레드 수
   * @param task 실행할 작업 (Runnable)
   * @return 실행 결과 (ExecutionResult)
   */
  @SuppressWarnings("unchecked")
  public static ExecutionResult run(int threadCount, Runnable task) {
    return run(threadCount, task, (Class<? extends Throwable>[]) new Class<?>[0]);
  }

  /**
   * 작업을 지정된 스레드 수만큼 동시에 실행하며, 허용된 예외는 실패 카운트로 집계합니다.
   *
   * @param threadCount 동시 실행할 스레드 수
   * @param task 실행할 작업 (Runnable)
   * @param expectedExceptionTypes 비즈니스 로직상 발생할 수 있는 '기대되는 실패' 예외 클래스 목록
   * @return 실행 결과 (ExecutionResult)
   */
  @SafeVarargs
  public static ExecutionResult run(
      int threadCount, Runnable task, Class<? extends Throwable>... expectedExceptionTypes) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger expectedFailureCount = new AtomicInteger(0);
    Map<Class<? extends Throwable>, AtomicInteger> failureCountByType = new ConcurrentHashMap<>();
    List<Throwable> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());
    List<Future<?>> futures = new ArrayList<>();

    try {
      for (int i = 0; i < threadCount; i++) {
        int threadId = i;
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await();
                    task.run();
                    successCount.incrementAndGet();
                  } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                      Thread.currentThread().interrupt();
                      log.error("Thread-{} interrupted", threadId);
                      unexpectedExceptions.add(e);
                    } else {
                      Class<? extends Throwable> matchedType =
                          findMatchingExpectedType(e, expectedExceptionTypes);
                      if (matchedType != null) {
                        expectedFailureCount.incrementAndGet();
                        failureCountByType
                            .computeIfAbsent(matchedType, k -> new AtomicInteger(0))
                            .incrementAndGet();
                      } else {
                        log.error("Unexpected exception in thread-{}", threadId, e);
                        unexpectedExceptions.add(e);
                      }
                    }
                  } finally {
                    doneLatch.countDown();
                  }
                }));
      }

      startLatch.countDown(); // Start all threads

      if (!doneLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new AssertionError(
            "Test timed out: threads did not complete within "
                + LATCH_TIMEOUT_SECONDS
                + " seconds");
      }

      // Ensure all futures are completed and check for execution exceptions
      for (Future<?> future : futures) {
        try {
          future.get(); // 이미 Latch에서 대기했으므로 즉시 리턴하거나 예외를 던져야 함
        } catch (ExecutionException e) {
          // Executor 내에서 잡히지 않은 예외가 있다면 수집 (보통 위 try-catch에서 다 잡힘)
          unexpectedExceptions.add(e.getCause());
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Test main thread interrupted", e);
    } finally {
      shutdownExecutorGracefully(executor);
    }

    // Convert AtomicInteger map to Integer map for immutable result
    Map<Class<? extends Throwable>, Integer> finalFailureCountByType = new HashMap<>();
    failureCountByType.forEach((k, v) -> finalFailureCountByType.put(k, v.get()));

    return new ExecutionResult(
        successCount.get(),
        expectedFailureCount.get(),
        unexpectedExceptions,
        finalFailureCountByType);
  }

  /**
   * 예외 체인을 순회하여 매칭되는 기대 예외 타입을 찾습니다.
   *
   * @return 매칭된 예외 클래스, 없으면 null
   */
  private static Class<? extends Throwable> findMatchingExpectedType(
      Throwable e, Class<? extends Throwable>[] expectedExceptionTypes) {
    Throwable current = e;
    while (current != null) {
      for (Class<? extends Throwable> expected : expectedExceptionTypes) {
        if (expected.isInstance(current)) {
          return expected;
        }
      }
      if (current == current.getCause()) { // Prevent infinite loop
        break;
      }
      current = current.getCause();
    }
    return null;
  }

  private static void shutdownExecutorGracefully(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public static class ExecutionResult {
    private final int successCount;
    private final int expectedFailureCount;
    private final List<Throwable> unexpectedExceptions;
    private final Map<Class<? extends Throwable>, Integer> failureCountByType;

    public ExecutionResult(
        int successCount,
        int expectedFailureCount,
        List<Throwable> unexpectedExceptions,
        Map<Class<? extends Throwable>, Integer> failureCountByType) {
      this.successCount = successCount;
      this.expectedFailureCount = expectedFailureCount;
      this.unexpectedExceptions = unexpectedExceptions;
      this.failureCountByType = failureCountByType;
    }

    public int getSuccessCount() {
      return successCount;
    }

    public int getExpectedFailureCount() {
      return expectedFailureCount;
    }

    /**
     * 특정 예외 타입의 발생 횟수를 반환합니다.
     *
     * @param exceptionType 조회할 예외 클래스
     * @return 해당 예외 발생 횟수 (없으면 0)
     */
    public int getExpectedFailureCountFor(Class<? extends Throwable> exceptionType) {
      return failureCountByType.getOrDefault(exceptionType, 0);
    }

    public Map<Class<? extends Throwable>, Integer> getFailureCountByType() {
      return Collections.unmodifiableMap(failureCountByType);
    }

    public List<Throwable> getUnexpectedExceptions() {
      return unexpectedExceptions;
    }

    public void assertNoUnexpectedExceptions() {
      if (!unexpectedExceptions.isEmpty()) {
        throw new AssertionError(
            "Unexpected exceptions occurred: " + unexpectedExceptions.get(0),
            unexpectedExceptions.get(0));
      }
    }
  }
}
