package com.securities.kuku.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TransactionTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");

  private Transaction createTransaction(TransactionStatus status) {
    return new Transaction(
        1L, TransactionType.DEPOSIT, "Test", "REF-001", status, null, FIXED_TIME);
  }

  @Nested
  @DisplayName("생성자 유효성 검증")
  class ConstructorValidation {

    @Test
    @DisplayName("정상적인 트랜잭션 생성")
    void success_whenAllFieldsValid() {
      Transaction transaction =
          new Transaction(
              1L,
              TransactionType.DEPOSIT,
              "Test Description",
              "REF-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      assertThat(transaction.getId()).isEqualTo(1L);
      assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(transaction.getDescription()).isEqualTo("Test Description");
      assertThat(transaction.getBusinessRefId()).isEqualTo("REF-001");
      assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.POSTED);
      assertThat(transaction.getReversalOfTransactionId()).isNull();
      assertThat(transaction.getCreatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("트랜잭션 타입이 없으면 예외가 발생한다")
    void throwsException_whenTypeIsNull() {
      assertThatThrownBy(
              () ->
                  new Transaction(
                      1L, null, "Test", "REF-001", TransactionStatus.POSTED, null, FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Type");
    }

    @Test
    @DisplayName("트랜잭션 상태가 없으면 예외가 발생한다")
    void throwsException_whenStatusIsNull() {
      assertThatThrownBy(
              () ->
                  new Transaction(
                      1L, TransactionType.DEPOSIT, "Test", "REF-001", null, null, FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Status");
    }

    @Test
    @DisplayName("생성 일시가 없으면 예외가 발생한다")
    void throwsException_whenCreatedAtIsNull() {
      assertThatThrownBy(
              () ->
                  new Transaction(
                      1L,
                      TransactionType.DEPOSIT,
                      "Test",
                      "REF-001",
                      TransactionStatus.POSTED,
                      null,
                      null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("CreatedAt");
    }

    @Test
    @DisplayName("description은 null을 허용한다")
    void allowsNullDescription() {
      Transaction transaction =
          new Transaction(
              1L,
              TransactionType.DEPOSIT,
              null,
              "REF-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      assertThat(transaction.getDescription()).isNull();
    }

    @Test
    @DisplayName("businessRefId는 null을 허용한다")
    void allowsNullBusinessRefId() {
      Transaction transaction =
          new Transaction(
              1L,
              TransactionType.DEPOSIT,
              "Test",
              null,
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      assertThat(transaction.getBusinessRefId()).isNull();
    }

    @Test
    @DisplayName("id는 null을 허용한다 (신규 생성 시)")
    void allowsNullId() {
      Transaction transaction =
          new Transaction(
              null,
              TransactionType.DEPOSIT,
              "Test",
              "REF-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      assertThat(transaction.getId()).isNull();
    }
  }

  @Nested
  @DisplayName("역분개 트랜잭션 생성 검증")
  class ReversalTransactionCreation {

    @Test
    @DisplayName("역분개 트랜잭션 생성 시 reversalOfTransactionId 설정")
    void success_withReversalOfTransactionId() {
      Transaction reversal =
          new Transaction(
              2L,
              TransactionType.DEPOSIT,
              "Reversal",
              "REF-001-REV",
              TransactionStatus.POSTED,
              1L,
              FIXED_TIME);

      assertThat(reversal.getReversalOfTransactionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("역분개 트랜잭션은 POSTED 상태여야 한다")
    void throwsException_whenReversalIsNotPosted() {
      assertThatThrownBy(
              () ->
                  new Transaction(
                      2L,
                      TransactionType.DEPOSIT,
                      "Reversal",
                      "REF-001",
                      TransactionStatus.PENDING,
                      1L,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Reversal transaction must be in POSTED state");
    }

    @Test
    @DisplayName("REVERSED 상태의 트랜잭션은 역분개 트랜잭션이 될 수 없다")
    void throwsException_whenReversedHasReversalId() {
      assertThatThrownBy(
              () ->
                  new Transaction(
                      2L,
                      TransactionType.DEPOSIT,
                      "Reversal",
                      "REF-001",
                      TransactionStatus.REVERSED,
                      1L,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A REVERSED transaction cannot be a reversal of another transaction");
    }
  }

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class FactoryMethods {

    @Test
    @DisplayName("createDeposit은 DEPOSIT 타입과 POSTED 상태로 생성한다")
    void createDeposit_success() {
      Transaction deposit = Transaction.createDeposit("입금", "DEP-001", FIXED_TIME);

      assertThat(deposit.getId()).isNull();
      assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(deposit.getDescription()).isEqualTo("입금");
      assertThat(deposit.getBusinessRefId()).isEqualTo("DEP-001");
      assertThat(deposit.getStatus()).isEqualTo(TransactionStatus.POSTED);
      assertThat(deposit.getReversalOfTransactionId()).isNull();
      assertThat(deposit.getCreatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("createWithdraw는 WITHDRAWAL 타입과 POSTED 상태로 생성한다")
    void createWithdraw_success() {
      Transaction withdrawal = Transaction.createWithdraw("출금", "WDR-001", FIXED_TIME);

      assertThat(withdrawal.getId()).isNull();
      assertThat(withdrawal.getType()).isEqualTo(TransactionType.WITHDRAWAL);
      assertThat(withdrawal.getDescription()).isEqualTo("출금");
      assertThat(withdrawal.getBusinessRefId()).isEqualTo("WDR-001");
      assertThat(withdrawal.getStatus()).isEqualTo(TransactionStatus.POSTED);
      assertThat(withdrawal.getReversalOfTransactionId()).isNull();
      assertThat(withdrawal.getCreatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("createReversal은 REVERSAL 타입과 원본 트랜잭션 ID를 설정한다")
    void createReversal_success() {
      Long originalTxId = 100L;

      Transaction reversal = Transaction.createReversal(originalTxId, "취소 사유", FIXED_TIME);

      assertThat(reversal.getId()).isNull();
      assertThat(reversal.getType()).isEqualTo(TransactionType.REVERSAL);
      assertThat(reversal.getDescription()).isEqualTo("취소 사유");
      assertThat(reversal.getBusinessRefId()).isEqualTo("reversal-100");
      assertThat(reversal.getStatus()).isEqualTo(TransactionStatus.POSTED);
      assertThat(reversal.getReversalOfTransactionId()).isEqualTo(originalTxId);
      assertThat(reversal.getCreatedAt()).isEqualTo(FIXED_TIME);
    }
  }

  @Nested
  @DisplayName("toReversed() 메서드")
  class ToReversed {

    @Test
    @DisplayName("POSTED 상태의 트랜잭션을 REVERSED로 변경한 새 인스턴스 반환")
    void success_whenStatusIsPosted() {
      Transaction original = createTransaction(TransactionStatus.POSTED);

      Transaction reversed = original.toReversed();

      assertThat(reversed).isNotSameAs(original);
      assertThat(reversed.getId()).isEqualTo(original.getId());
      assertThat(reversed.getStatus()).isEqualTo(TransactionStatus.REVERSED);
      assertThat(reversed.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    @ParameterizedTest
    @EnumSource(
        value = TransactionStatus.class,
        names = {"PENDING", "REVERSED", "UNKNOWN"})
    @DisplayName("POSTED가 아닌 상태는 역분개 불가")
    void throwsException_whenStatusIsNotPosted(TransactionStatus status) {
      Transaction tx = createTransaction(status);

      assertThatThrownBy(tx::toReversed).isInstanceOf(InvalidTransactionStateException.class);
    }
  }

  @Nested
  @DisplayName("markAsUnknown() 메서드")
  class MarkAsUnknown {

    @Test
    @DisplayName("PENDING 상태의 트랜잭션을 UNKNOWN으로 변경한 새 인스턴스 반환")
    void success_whenStatusIsPending() {
      Transaction pending = createTransaction(TransactionStatus.PENDING);

      Transaction unknown = pending.markAsUnknown();

      assertThat(unknown).isNotSameAs(pending);
      assertThat(unknown.getStatus()).isEqualTo(TransactionStatus.UNKNOWN);
      assertThat(unknown.getId()).isEqualTo(pending.getId());
    }

    @ParameterizedTest
    @EnumSource(
        value = TransactionStatus.class,
        names = {"POSTED", "REVERSED", "UNKNOWN"})
    @DisplayName("PENDING이 아닌 상태는 UNKNOWN으로 전환 불가")
    void throwsException_whenStatusIsNotPending(TransactionStatus status) {
      Transaction tx = createTransaction(status);

      assertThatThrownBy(tx::markAsUnknown)
          .isInstanceOf(InvalidTransactionStateException.class)
          .hasMessageContaining("PENDING");
    }
  }

  @Nested
  @DisplayName("resolveUnknown() 메서드")
  class ResolveUnknown {

    @Test
    @DisplayName("UNKNOWN 상태의 트랜잭션을 POSTED로 해결")
    void success_toPosted() {
      Transaction unknown = createTransaction(TransactionStatus.UNKNOWN);

      Transaction resolved = unknown.resolveUnknown(TransactionStatus.POSTED);

      assertThat(resolved).isNotSameAs(unknown);
      assertThat(resolved.getStatus()).isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    @DisplayName("UNKNOWN 상태는 REVERSED로 직접 해결할 수 없다")
    void throwsException_toReversed() {
      Transaction unknown = createTransaction(TransactionStatus.UNKNOWN);

      assertThatThrownBy(() -> unknown.resolveUnknown(TransactionStatus.REVERSED))
          .isInstanceOf(InvalidTransactionStateException.class);
    }

    @ParameterizedTest
    @EnumSource(
        value = TransactionStatus.class,
        names = {"PENDING", "POSTED", "REVERSED"})
    @DisplayName("UNKNOWN이 아닌 상태에서는 resolveUnknown 호출 불가")
    void throwsException_whenStatusIsNotUnknown(TransactionStatus status) {
      Transaction tx = createTransaction(status);

      assertThatThrownBy(() -> tx.resolveUnknown(TransactionStatus.POSTED))
          .isInstanceOf(InvalidTransactionStateException.class)
          .hasMessageContaining("UNKNOWN");
    }
  }

  @Nested
  @DisplayName("confirm() 메서드")
  class Confirm {

    @Test
    @DisplayName("PENDING 상태의 트랜잭션을 POSTED로 확정한 새 인스턴스 반환")
    void success_whenStatusIsPending() {
      Transaction pending = createTransaction(TransactionStatus.PENDING);

      Transaction confirmed = pending.confirm();

      assertThat(confirmed).isNotSameAs(pending);
      assertThat(confirmed.getStatus()).isEqualTo(TransactionStatus.POSTED);
      assertThat(confirmed.getId()).isEqualTo(pending.getId());
      assertThat(confirmed.getType()).isEqualTo(pending.getType());
      assertThat(confirmed.getCreatedAt()).isEqualTo(pending.getCreatedAt());
    }

    @ParameterizedTest
    @EnumSource(
        value = TransactionStatus.class,
        names = {"POSTED", "REVERSED", "UNKNOWN"})
    @DisplayName("PENDING이 아닌 상태는 confirm 불가")
    void throwsException_whenStatusIsNotPending(TransactionStatus status) {
      Transaction tx = createTransaction(status);

      assertThatThrownBy(tx::confirm)
          .isInstanceOf(InvalidTransactionStateException.class)
          .hasMessageContaining("PENDING");
    }
  }

  @Nested
  @DisplayName("createJournalEntry() 메서드")
  class CreateJournalEntry {

    private static final Long ACCOUNT_ID = 100L;
    private static final BigDecimal AMOUNT = new BigDecimal("1000");

    @Test
    @DisplayName("DEPOSIT 트랜잭션은 CREDIT JournalEntry를 생성한다")
    void deposit_createsCreditEntry() {
      Transaction deposit =
          new Transaction(
              1L,
              TransactionType.DEPOSIT,
              "입금",
              "REF-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      JournalEntry entry = deposit.createJournalEntry(ACCOUNT_ID, AMOUNT, FIXED_TIME);

      assertThat(entry.getEntryType()).isEqualTo(JournalEntry.EntryType.CREDIT);
    }

    @Test
    @DisplayName("WITHDRAWAL 트랜잭션은 DEBIT JournalEntry를 생성한다")
    void withdrawal_createsDebitEntry() {
      Transaction withdrawal =
          new Transaction(
              2L,
              TransactionType.WITHDRAWAL,
              "출금",
              "REF-002",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      JournalEntry entry = withdrawal.createJournalEntry(ACCOUNT_ID, AMOUNT, FIXED_TIME);

      assertThat(entry.getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
    }

    @Test
    @DisplayName("지원하지 않는 TransactionType은 예외를 발생시킨다")
    void unsupportedType_throwsException() {
      Transaction reversal =
          new Transaction(
              3L,
              TransactionType.REVERSAL,
              "역분개",
              "REF-003",
              TransactionStatus.POSTED,
              1L,
              FIXED_TIME);

      assertThatThrownBy(() -> reversal.createJournalEntry(ACCOUNT_ID, AMOUNT, FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("REVERSAL");
    }
  }

  @Nested
  @DisplayName("toPostedEvent() 메서드")
  class ToPostedEvent {

    @Test
    @DisplayName("DEPOSIT 트랜잭션의 LedgerPostedEvent 생성 - 모든 필드 정확성")
    void deposit_createsPostedEventWithCorrectFields() {
      // Given
      Transaction deposit =
          new Transaction(
              100L,
              TransactionType.DEPOSIT,
              "입금",
              "DEP-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);
      Long accountId = 1L;
      BigDecimal amount = new BigDecimal("1000");

      // When
      var event = deposit.toPostedEvent(accountId, amount, TransactionType.DEPOSIT);

      // Then
      assertThat(event.transactionId()).isEqualTo(100L);
      assertThat(event.accountId()).isEqualTo(1L);
      assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("1000"));
      assertThat(event.transactionType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(event.occurredAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("WITHDRAWAL 트랜잭션의 LedgerPostedEvent 생성")
    void withdrawal_createsPostedEventWithCorrectType() {
      // Given
      Transaction withdrawal =
          new Transaction(
              200L,
              TransactionType.WITHDRAWAL,
              "출금",
              "WDR-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);
      Long accountId = 2L;
      BigDecimal amount = new BigDecimal("500");

      // When
      var event = withdrawal.toPostedEvent(accountId, amount, TransactionType.WITHDRAWAL);

      // Then
      assertThat(event.transactionId()).isEqualTo(200L);
      assertThat(event.transactionType()).isEqualTo(TransactionType.WITHDRAWAL);
      assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("트랜잭션 내부 상태(id, createdAt)를 이벤트에 올바르게 전달")
    void usesInternalStateForEvent() {
      // Given
      Instant customTime = Instant.parse("2025-06-15T12:00:00Z");
      Transaction tx =
          new Transaction(
              999L,
              TransactionType.DEPOSIT,
              "테스트",
              "REF-999",
              TransactionStatus.POSTED,
              null,
              customTime);

      // When
      var event = tx.toPostedEvent(10L, new BigDecimal("100"), TransactionType.DEPOSIT);

      // Then - 내부 상태가 정확히 전달되는지 검증
      assertThat(event.transactionId()).isEqualTo(999L);
      assertThat(event.occurredAt()).isEqualTo(customTime);
    }
  }

  @Nested
  @DisplayName("toReversedEvent() 메서드")
  class ToReversedEvent {

    @Test
    @DisplayName("역분개 이벤트 생성 - 모든 필드 정확성")
    void createsReversedEventWithCorrectFields() {
      // Given
      Transaction reversal =
          new Transaction(
              300L,
              TransactionType.REVERSAL,
              "취소 사유",
              "reversal-100",
              TransactionStatus.POSTED,
              100L,
              FIXED_TIME);
      Long originalTxId = 100L;
      String reason = "고객 요청";

      // When
      var event = reversal.toReversedEvent(originalTxId, reason);

      // Then
      assertThat(event.reversalTransactionId()).isEqualTo(300L);
      assertThat(event.originalTransactionId()).isEqualTo(100L);
      assertThat(event.reason()).isEqualTo("고객 요청");
      assertThat(event.occurredAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("역분개 이벤트는 트랜잭션 내부 시간(createdAt)을 사용")
    void usesTransactionCreatedAtForTimestamp() {
      // Given
      Instant reversalTime = Instant.parse("2025-12-01T10:30:00Z");
      Transaction reversal =
          new Transaction(
              500L,
              TransactionType.REVERSAL,
              "역분개",
              "reversal-200",
              TransactionStatus.POSTED,
              200L,
              reversalTime);

      // When
      var event = reversal.toReversedEvent(200L, "시스템 오류");

      // Then
      assertThat(event.occurredAt()).isEqualTo(reversalTime);
      assertThat(event.reversalTransactionId()).isEqualTo(500L);
    }
  }
}
