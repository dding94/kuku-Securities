# Week 4 회고: 원장 시스템 검증 테스트

> **기간**: 2024.12.18 ~ 2024.12.31 (약 2주)  
> **주제**: PENDING/UNKNOWN 상태, 동시성 테스트, Outbox 패턴 설계

---

## 🎯 이번 주 목표 달성도

**계획 vs 실제**: Week 4는 원래 1주일 분량이었으나, 면접 일정과 병행하면서 **약 2주에 걸쳐 진행**되었다.

### 완료된 항목
- [x] TransactionStatus.UNKNOWN 상태 추가 (PR 1)
- [x] PENDING → POSTED 2단계 전환 로직 (PR 2)
- [x] 동시성 테스트 - LedgerConcurrencyTest (PR 3)
- [x] Optimistic Lock 실패 Retry 전략 - @Retryable (PR 4)
- [x] Outbox 패턴 설계 - LedgerPostedEvent, LedgerReversedEvent (PR 6)
- [x] C4 Component Diagram 작성
- [x] 패키지 구조 문서화

### 이관된 항목
- DB Lock 경쟁 테스트 → Week 6 (동시성 제어 주차와 통합)
- Kafka 연동 및 검증 → Week 7 (E2E 플로우와 함께)

### 교훈
> 면접, 개인 일정 등 외부 요인으로 인해 일정이 밀릴 수 있다. 주차별 버퍼를 고려하거나, 범위를 사전에 조정하는 유연함이 필요하다.

---

## ✅ 잘한 점 (Keep)

### 1. TDD 방식의 개발
- 테스트를 먼저 작성하고 구현하는 방식을 유지했다.
- `ConfirmTransactionServiceTest`, `LedgerConcurrencyTest` 등에서 RED → GREEN → REFACTOR 사이클을 따랐다.

### 2. YAGNI 원칙 준수
- 필요한 최소한의 기능만 구현하려고 노력했다.
- 예: Kafka Producer는 Week 7로 미루고, Outbox **설계**만 완료
- 예: LockConflictException 같은 미사용 클래스는 만들지 않음

### 3. 도메인 이벤트 생성 책임 명확화
- `Transaction` 엔티티가 자신의 도메인 이벤트(`toLedgerPostedEvent()`)를 생성하도록 결정
- ADR 문서로 의사결정 과정을 기록 (`009-domain-event-creation-pattern.md`)

---

## 🔧 개선할 점 (Problem)

### 1. 금융 도메인 지식 부족
- 이중부기, 역분개 같은 개념을 처음 접하면서 학습 시간이 많이 소요됨
- 실제 증권사에서 사용하는 용어나 프로세스에 대한 이해가 부족함
- **개선 방안**: 금융 도메인 서적 또는 블로그 정리 글 읽기, 실제 증권사 API 문서 참고

### 2. 테스트 코드 작성 기준 모호
- "한 가지 테스트에서는 한 가지 목적의 검증"이라는 원칙은 있지만, 실제 적용 시 경계가 모호했음
- 예: Mock 설정과 실제 로직 검증의 균형
- **개선 방안**: 팀 컨벤션처럼 나만의 명확한 테스트 작성 가이드라인 정리 필요

### 3. 도메인 이벤트 생성의 트레이드오프 기준
- "서비스에서 생성 vs 엔티티에서 생성"에 대한 고민이 오래 걸림
- ADR로 결정은 했지만, 다른 상황에서도 적용 가능한 범용 기준이 필요함
- **개선 방안**: 다른 오픈소스 프로젝트에서 어떻게 처리하는지 리서치

---

## 💡 시도해볼 것 (Try)

### 1. 민첩하게 행동하기
- 고민하는 시간이 많은 것은 좋지만, 완벽을 추구하다 보면 진행이 늦어진다.
- **액션**: "완벽하게 하려고 하지 말고, 먼저 해보고 개선하자"
- 주차별 목표를 **최대한 완수**하고, 부족한 점은 회고에서 발견하여 다음 주차에 반영

### 2. 타임박싱(Timeboxing) 적용
- 의사결정에 시간 제한을 두기 (예: 30분 고민 후 결정)
- 코드 리뷰나 리팩토링에도 시간 제한 두기

### 3. 면접 준비와 프로젝트 병행 전략
- 면접 일정이 있는 주는 미리 범위를 축소하거나, 문서화 작업 위주로 진행

---

## 📝 배운 점 / 기술 인사이트

### 1. Outbox 패턴
- **무엇을 배웠나**: 분산 시스템에서 데이터 일관성을 보장하면서 이벤트를 발행하는 방법
- **왜 필요한가**: Kafka가 죽어도 트랜잭션은 성공해야 하고, 이벤트는 나중에 발행되어야 함
- **핵심 인사이트**: "트랜잭션과 이벤트 발행을 동일 DB 트랜잭션으로 묶으면 At-Least-Once 보장 가능"

### 2. 도메인 이벤트 생성 책임
- **결정**: Aggregate Root(Transaction)가 자신의 이벤트를 생성
- **이유**: "Tell, Don't Ask" 원칙, 정보 은닉, DDD 철학
- **참고**: ADR-009에 상세 기록

### 3. Optimistic Lock과 Retry 전략
- **배운 것**: Spring Retry의 `@Retryable` 사용법과 Exponential Backoff
- **인사이트**: 재시도 횟수와 간격은 비즈니스 요구사항에 따라 결정해야 함
- **주의점**: 주문 체결 같은 금융 거래에서는 재시도가 오히려 위험할 수 있음

---

## 📊 Week 4 산출물

| 산출물 | 경로 |
|:------|:-----|
| ADR: Retry 전략 | `/docs/adr/007-retry-strategy.md` |
| ADR: Outbox 패턴 | `/docs/adr/008-outbox-pattern.md` |
| ADR: 도메인 이벤트 생성 패턴 | `/docs/adr/009-domain-event-creation-pattern.md` |
| Diagram: C4 Component | `/docs/diagrams/c4-component-ledger.md` |
| Diagram: Ledger 이벤트 흐름 | `/docs/diagrams/ledger-event-flow.md` |

---

## 🚀 Week 5 목표

- [ ] 주문 상태 머신(State Machine) 설계
- [ ] OrderController 구현 (REST API)
- [ ] 예수금/보유수량 검증 로직

---

> "완벽을 추구하되, 완벽에 매몰되지 말자. 먼저 해보고, 개선하자."
