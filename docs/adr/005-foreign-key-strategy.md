# [ADR-001] 증권 원장 시스템의 Foreign Key 사용 전략

*   **Status**: Accepted
*   **Date**: 2025-12-05
*   **Author**: dding94

## 1. Context (배경)

우리는 **초당 수만 건 이상의 주문과 체결**이 발생하는 증권 거래 플랫폼을 구축하고 있습니다. 이 시스템의 핵심인 **원장(Ledger)** 도메인은 고객의 자산(돈과 주식)을 다루므로 **데이터 무결성(Data Integrity)**이 무엇보다 중요합니다.

전통적인 RDBMS 설계에서 데이터 무결성을 보장하는 가장 강력한 수단은 **Foreign Key (FK) 제약조건**입니다. 하지만, 고동시성(High Concurrency) 환경에서 FK는 심각한 성능 저하와 장애의 원인이 될 수 있다는 딜레마가 존재합니다.

우리는 **"데이터의 정확성"**과 **"시스템의 안정성/성능"** 사이에서 명확한 기준을 수립해야 합니다.

## 2. Decision (결정)

1.  **Core Ledger (원장/주문/체결)**: **물리적 Foreign Key를 사용하지 않는다 (Logical FK 사용).**
    *   대상 테이블: `transactions`, `journal_entries`, `balances`, `orders`, `trades` 등.
    *   데이터 정합성은 **애플리케이션 계층(Service Layer)**의 검증 로직과 **비동기 리콘실리에이션(Reconciliation)** 프로세스로 보장한다.
    *   **Isolation Level**: **READ COMMITTED**를 사용하여 Gap Lock 발생을 원천 차단한다.
2.  **Reference Data (기준 정보)**: **물리적 Foreign Key를 사용한다.**
    *   대상 테이블: `symbols`, `exchanges`, `currencies` 등.
    *   변경이 드물고 참조 무결성이 중요한 메타 데이터는 DB의 기능을 활용한다.

## 3. Detailed Analysis (상세 분석)

### 3.1. 물리적 FK를 사용해야 하는 이유 (Pros)

*   **강력한 무결성 보장**: 개발자의 실수나 애플리케이션 버그로 인해 "존재하지 않는 계좌의 거래 내역" 같은 고아 데이터(Orphaned Row)가 생기는 것을 DB 레벨에서 원천 차단합니다.
*   **데이터 모델의 명확성**: ERD만 봐도 테이블 간의 관계를 명확히 알 수 있으며, DB 툴의 지원을 받을 수 있습니다.

### 3.2. 물리적 FK를 사용하지 말아야 하는 이유 (Cons) - **Critical**

증권 시스템에서 물리적 FK가 위험한 이유는 다음과 같습니다.

#### A. 데드락(Deadlock)과 트랜잭션 설계 (Root Cause Analysis)
FK는 데드락의 **트리거(Trigger)**일 뿐, 근본 원인은 **잘못된 락 순서(Lock Ordering)**와 **트랜잭션 범위**에 있습니다.
하지만 물리적 FK는 부모 테이블(`accounts`)에 강제로 **Shared Lock (S-Lock)**을 걸기 때문에, 애플리케이션 레벨에서 락 순서를 제어하는 것을 불가능하게 만듭니다.
*   **FK 존재 시**: `Journal Insert` (S-Lock on Account) <-> `Balance Update` (X-Lock on Account) 경합 발생.
*   **FK 제거 시**: `Journal Insert`는 `Account`를 건드리지 않음. `Balance Update`만 독립적으로 수행됨. -> **데드락 원천 차단.**

#### B. 쓰기 성능(Throughput) 저하
모든 `INSERT`/`UPDATE`마다 부모 테이블을 조회(Select)하는 비용이 발생합니다. 트래픽이 폭주하는 장 시작/마감 동시호가 시간에 이는 DB CPU를 불필요하게 소모합니다.

#### C. 샤딩(Sharding)을 위한 미래 대비 (Future-Proofing)
현재 당장 샤딩이 필요한 것은 아닙니다. 하지만 금융 시스템은 데이터가 기하급수적으로 증가하므로, 언젠가는 `Account ID`를 기준으로 수평 분할(Sharding)을 해야 합니다.
물리적 FK는 샤딩의 가장 큰 걸림돌입니다. **"지금 당장 필요 없어도, 나중에 발목 잡지 않는 설계"**를 위해 미리 FK를 제거하는 것이 합리적입니다.

### 3.3. Hidden Risk: Gap Locks & Isolation Level (잠재적 위험: 갭 락과 격리 수준)

물리적 FK를 제거하더라도, MySQL InnoDB의 기본 격리 수준인 **REPEATABLE READ**에서는 Secondary Index(`account_id`)에 대해 **Gap Lock / Next-Key Lock**이 발생할 수 있습니다.

*   **Problem**: `journal_entries` 테이블의 `account_id` 인덱스에 대해 범위 락이 걸리면, 해당 범위에 속하는 다른 계좌의 입출금까지 블로킹될 수 있습니다.
*   **Solution**: **READ COMMITTED** 격리 수준을 사용합니다.
    *   `READ COMMITTED`에서는 (FK 체크나 중복 키 체크를 제외하고는) Gap Lock이 비활성화됩니다.
    *   따라서 "FK 제거 + READ COMMITTED" 조합이어야만 진정한 의미의 Lock Free(에 가까운) 동시성을 확보할 수 있습니다.

#### 부작용(Side Effect) 및 대응 방안
`READ COMMITTED`를 사용하면 **Non-Repeatable Read** (한 트랜잭션 내에서 같은 데이터를 두 번 읽었을 때 값이 달라짐) 현상이 발생할 수 있습니다.
*   **Risk**: 잔고(`Balance`) 조회 후 업데이트 시 정합성 깨짐 우려.
*   **Mitigation**: `Balance` 엔티티에 **`@Version` (Optimistic Locking)**을 적용하여, 격리 수준과 무관하게 데이터 갱신의 원자성을 보장합니다.

## 4. Consequences (결과 및 대응)

물리적 FK를 제거함에 따라 발생하는 리스크는 다음과 같이 관리합니다.

1.  **Application Validation (코드 레벨 검증)**:
    *   모든 `INSERT` 전, 캐시(Redis/Caffeine)를 통해 참조 ID(`account_id`)의 유효성을 검증합니다.
    ```java
    @Service
    public class JournalService {
        private final AccountRepository accountRepository;
        // Local Cache for high-throughput validation
        private final Cache<Long, Boolean> accountExistenceCache; 

        public void appendJournal(JournalEntry entry) {
            // 1. App-Level Validation (Fast & Non-Blocking)
            boolean exists = accountExistenceCache.get(entry.getAccountId(), 
                id -> accountRepository.existsById(id));
            
            if (!exists) {
                throw new InvalidAccountException(entry.getAccountId());
            }
            
            // 2. DB Insert (No Physical FK Lock)
            journalRepository.save(entry);
        }
    }
    ```

2.  **Real-time Monitoring & Compensation (실시간 감지 및 보상)**:
    *   단순 배치가 아닌, **CDC (Change Data Capture)** 기반의 실시간 파이프라인을 구축합니다.
    *   **Debezium**으로 `journal_entries` 변경분을 캡처 -> **Kafka Streams**에서 `accounts` 토픽과 조인하여 고아 데이터 감지 -> **Alert** 발송 및 보상 트랜잭션 수행.
3.  **Test Coverage**:
    *   통합 테스트(Integration Test)를 통해 데이터 관계가 올바르게 저장되는지 철저히 검증합니다.

## 5. Conclusion (결론)

증권 시스템의 최우선 가치는 **"멈추지 않는 거래"**입니다.
데이터 무결성은 매우 중요하지만, 그것이 **시스템 전체를 멈추게 하는(Deadlock) 원인**이 되어서는 안 됩니다.
따라서 우리는 **Core 도메인에 한해 물리적 제약조건을 포기하고, 애플리케이션 레벨에서 무결성을 책임지는 전략**을 선택합니다.
