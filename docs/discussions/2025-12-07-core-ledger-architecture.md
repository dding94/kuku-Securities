# 💬 Core Ledger 아키텍처 기술 토의 (가상 시뮬레이션)

**일시**: 2025-12-07
**참석자**:
- **Dave (Principal Architect)**: 시스템 회복 탄력성(Resilience), "Why-Driven" 의사결정, MSA 전체 일관성 담당.
- **Alice (Lead Domain Engineer)**: 금융 데이터 무결성, 이중 부기 원칙, 비즈니스 로직 수호자.
- **Bob (Senior Infra/Performance Engineer)**: 처리량(Throughput), 레이턴시(Latency), DB 튜닝, 동시성 제어 덕후.

---

## 주제 1: 물리적 Foreign Key 미사용 정책에 대하여

**Bob**: `README`를 보니까 MySQL에서 **물리적 Foreign Key(FK)를 사용하지 않는다**고 명시되어 있네요. 쓰기 성능 관점에서 전적으로 찬성입니다. 전 직장(HFT 샵)에서도 피크 타임에 데드락 주범이 항상 FK 체크였거든요.

**Alice**: Bob, 성능 이슈는 이해하지만 이건 **원장(Ledger)**이에요. 우리 핵심 가치 3번이 '데이터 무결성'이잖아요? FK가 없으면, 존재하지 않는 `Account`를 가리키는 `JournalEntry`가 생기거나, 없는 `Transaction`을 역분개하려는 시도를 DB 레벨에서 어떻게 막죠?

**Dave**: Alice의 우려도 타당합니다. 하지만 `목표.md`를 다시 보죠. 우리는 **"회복 탄력성(Resilience)"**과 **"고동시성(High Concurrency)"**을 최우선으로 합니다. DB 레벨의 강한 제약조건(Hard Constraints)은 스케일링 상황에서 원장 서비스를 멈추게 하는 병목이 될 가능성이 큽니다. 제 생각은, 무결성 검증은 **애플리케이션 계층(Service/Domain)**에서 수행하고, 최종적인 정합성은 **배치(Batch) 프로세스**로 보정해야 한다고 봅니다.

**Alice**: 애플리케이션 검증만으로는 불안해요. 안전장치가 필요합니다. DB 제약조건을 뺄 거라면, 엄격한 `고아 데이터 정리(Orphaned Data Cleanup)` 배치 정책이나 비동기 무결성 체커가 반드시 있어야 해요. 적어도 `Ledger` 서비스가 `JournalEntry`를 쓰기 전에 `accountRepository.existsById` 같은 존재 여부 체크는 필수로 하자는 건 합의해 주실 거죠?

**Bob**: `existsById`는 쓰기 전에 읽기를 한 번 더 하는 건데... 뭐, `Account` 테이블은 읽기 위주이고 `Journal/Transaction`은 쓰기 위주니까 그 정도 오버헤드는 괜찮겠네요. 정 느려지면 `Account` 존재 여부를 캐싱하거나 Bloom Filter를 쓸 수도 있고요. 좋습니다, **논리적 FK(Logical FKs)**만 사용하는 걸로 하죠.

**Dave**: 합의되었습니다. 결정 사항: **물리적 FK는 사용하지 않는다**. 대신 이로 인한 리스크를 `ADR`에 명기하고, 도메인 서비스에서 엄격하게 검증 로직을 구현합시다.

---

## 주제 2: 이중 부기(Double-Entry)와 역분개(Reversal) 패턴

**Alice**: 코어 모델로 넘어가죠. `JournalEntry`에서 금액(`amount`)은 항상 양수로 하고 `DEBIT/CREDIT` Enum으로 차대변을 나누는 설계는 아주 정석적이라 마음에 듭니다. 특히 수정 대신 **역분개(Reversal, Copy-on-Write)** 패턴을 채택한 건 신의 한 수네요. 금융 거래를 "삭제"한다는 건 범죄나 다름없으니까요.

**Dave**: `Transaction` 엔티티가 `reversalOfTransactionId`로 자기 자신(Self-reference)을 참조하게 해서 감사 추적(Audit Trail) 그래프를 만드는 것도 영리한 설계입니다. 그런데 `README`에 있는 `Asset Hold(자산 동결)` 로직에 대해 질문이 있습니다.

**Bob**: 주문 시스템이 원장으로 동기(Sync) 요청을 보내는 부분 말씀이시죠?

**Dave**: 네. 만약 `주문 시스템`이 `Ledger.hold()`를 동기적으로 호출하는데, `Ledger`가 죽어 있다면 주문 자체가 불가능해집니다. 강결합(Tight Coupling)이 발생해요. `Hold` 요청도 Kafka로 비동기 처리해야 하지 않을까요?

**Bob**: `Hold`를 비동기로 하면, 사용자는 "주문 접수됨"을 보고 안심했는데 2초 뒤에 "잔액 부족으로 거부됨" 알림을 받을 수 있어요. 코인 거래소라면 몰라도, 주식 트레이딩에서는 매수 가능 금액(Buying Power) 확인은 즉각적이어야 UX가 박살나지 않습니다.

**Alice**: 도메인 관점에서도 `Hold`는 **자금의 임시 차단**입니다. 이걸 비동기로 처리하면 짧은 찰나에 주문이 여러 개 몰릴 때 잔고 이상의 주문이 접수되는 **이중 지불(Double Spend)** 리스크가 커져요. 여기서는 `Balance`에 대한 동기적 락(혹은 매우 빠른 일관성 체크)이 필수입니다.

**Dave**: 일리 있는 지적입니다. `목표.md`에 **낙관적 락(Optimistic Lock, `Balance.version`)** 언급이 있었죠.

**Alice**: 그리고 금융 시스템은 이 **"동기 호출"**에서 발생하는 지연과 실패를 어떻게 처리하느냐가 실력의 척도입니다. 단순히 타임아웃만 걸면 되는 게 아니에요.
1.  **연쇄 장애(Cascading Failure)**: Ledger가 느려지면 Order 스레드도 같이 묶여서 전체가 죽습니다. **Resilience4j**로 Circuit Breaker와 짧은 타임아웃(0.5초)을 강제해야 합니다.
2.  **"알 수 없음" 상태(Unknown State)**: Order가 `hold()`를 보냈는데 응답을 못 받은 경우, 실제 돈이 묶였는지 아닌지 모릅니다. 이때 **멱등성(Idempotency Key)**이 필수입니다. Order가 재시도(Retry)할 때 Ledger는 "이미 처리된 주문"임을 알고 성공 응답을 줘야 하죠.

**Dave**: 완벽합니다. 그럼 **하이브리드 전략(Hybrid Strategy)**으로 확정합시다.
- **Validation Phase (Sync)**: `Order` -> `Ledger.hold()` (동기). 가용 잔고 확인 및 동결. (Circuit Breaker 필수)
- **Execution Phase (Async)**: 체결 후 실제 자산 이동(Settlement)은 Kafka를 통해 비동기로 처리.

**Bob**: 동의합니다. 그리고 **역분개**는 "Append-only" 작업이라서 불변(Immutable) 원장 철학이랑 아주 잘 맞아요. 나중에 DB가 너무 커지면 Vacuuming이 필요하겠지만, 지금 아키텍처로는 이게 제일 안전합니다.

---

## 주제 3: 동시성(Concurrency)과 "잔고 스냅샷(Balance Snapshot)"

**Bob**: `Balance` 얘기가 나와서 말인데, `README`에서는 `Balance` 엔티티를 "성능을 위한 스냅샷"이라고 정의했더군요. 저는 `hold_amount` 필드가 좀 거슬립니다.

**Alice**: 왜요? `가용 잔고(Available Balance) = 전체 잔고(Balance.amount) - 묶인 돈(Balance.hold_amount)`. 직관적이잖아요.

**Bob**: 그 말은 주문이 살아있는 동안에는 계속 `Balance` 로우를 업데이트해야 한다는 뜻이거든요. 단순히 `hold_amount` 표기를 위해서요. 이건 활성 유저의 계좌에 대해 **Hot Row(특정 행에 대한 경합)**를 유발합니다. 초단타 트레이더가 초당 100건 주문을 날리면, 그 100건이 전부 같은 `Balance` 로우의 락을 잡으려고 싸울 겁니다.

**Dave**: 맞아요. 낙관적 락(`@Version`) 때문에 `ObjectOptimisticLockingFailureException`이 엄청 터져서 재시도(Retry) 하느라 CPU를 낭비할 수도 있겠네요.

**Bob**: 그렇죠. 이상적으로는 `Hold` 내역을 별도 테이블(`AssetHold` 엔티티)로 분리하고, 주문 시에는 `AssetHold`에만 insert 하고 메인 `Balance` 로우는 건드리지 말아야 합니다. 정산(Settlement) 시점에만 합치는 거죠.

**Alice**: 잠깐만요, `Balance`를 안 건드리면 가용 잔고 계산을 어떻게 효율적으로 하죠? `Balance.amount - sum(AssetHold where status='HELD')`? 주문이 수천 건 쌓여 있을 때 매번 sum 쿼리를 날리면 더 느려질 텐데요.

**Bob**: Redis에 "총 Hold 금액"만 캐싱해 두거나... 음, 네. 일단은 쿼리 단순화를 위해 `Balance`를 업데이트하는 비용을 감수하는 게 맞을 수도 있겠네요. 하지만 `AssetHold`를 자식 테이블로 두는 건 좋지만, 실시간 집계(Aggregation)는 비싸다는 걸 기억해야 합니다.

**Dave**: 현재 설계를 유지하죠: `Balance`에 `hold_amount`를 포함한다. 만약 Week 9 부하 테스트 때 경합이 문제가 되면, 그때 `hold_amount` 관리만 Redis(Atomic Increment)로 빼서 핫패스(Hot-path) 최적화를 합시다. 지금은 MySQL의 ACID 정합성이 더 안전합니다.

---

## 주제 4: 헥사고날 아키텍처와 정책

**Dave**: 마지막으로 `POLICY.md`. 우리 **헥사고날 아키텍처** 쓰는 거 맞죠? UseCase에는 `Command` 객체 쓰고, 모든 건 `Port`를 통하고.

**Alice**: `LoadAccountPort`랑 `WithdrawUseCase` 코드 봤는데, 단순 CRUD 치고는 좀 장황하긴 하더라고요. 하지만 `Ledger`니까 필요하다고 봐요. "코어 도메인"이랑 "웹 어댑터"를 완전히 격리해야 하니까요.

**Bob**: 제 걱정은 객체 생성 오버헤드입니다. 요청 하나 들어올 때마다 `DepositCommand` 만들고, `SelfValidating` 체크하고, DTO 매핑하고...

**Dave**: Bob, 우린 지금 게시판 만드는 게 아니라 증권사 원장을 만들고 있어요. 객체 생성 비용은 논리적 오류를 수정하는 비용에 비하면 0에 가깝습니다. `Command` 패턴을 쓰면 **애초에 잘못된 상태의 데이터가 UseCase로 진입조차 못 하게** 막을 수 있어요. 그 정도 나노초(NS)는 투자할 가치가 있습니다.

**Alice**: 그리고 `POLICY.md`에 **"결정론적 테스트(Deterministic Testing)"** 언급이 있던데, 시간이나 랜덤 값을 주입받아서 테스트하라는 거요. 이거 원장에서는 필수입니다. `LocalDateTime.now()`가 1밀리초 차이 난다고 테스트 깨지면 안 되거든요.

**Bob**: 알겠습니다. GC(Garbage Collection) 부하 안 가게 `Command` 객체는 가벼운 POJO로 유지할게요. 그리고 `Port` 밖으로 `AccountJpaEntity` 같은 DB 엔티티가 새어 나가지 않도록 철저히 검수하겠습니다.

---

## 결론 및 액션 아이템

1.  **DB 설계**: **물리적 FK 사용 안 함** 확정. 대신 도메인 서비스에서 `existsById` 등으로 데이터 무결성 체크 구현.
2.  **동시성**: `Balance`에 대한 **낙관적 락** 유지. Week 9 부하 테스트 시 `hold_amount` 경합 모니터링.
3.  **역분개**: Copy-on-Write 방식의 역분개 패턴 승인.
4.  **아키텍처**: 헥사고날 아키텍처 엄수. 상태를 변경하는 모든 UseCase는 `Command` 객체 사용.

**Dave**: "여러분, 이 정도면 꽤 탄탄한 기반이 마련된 것 같네요. `목표.md`의 'Why'를 충실히 따르면서, 데이터 무결성을 최우선으로 하되 고동시성을 위한 길도 열어둔 설계입니다."
