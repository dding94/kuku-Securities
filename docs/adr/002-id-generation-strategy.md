# ADR 002: ID Generation Strategy for Distributed Ledger

## Status
Proposed

## Context
원장(Ledger) 시스템의 데이터 모델 설계 중, Primary Key(PK) 생성 전략에 대한 고민이 제기됨.
초기 설계는 MySQL `AUTO_INCREMENT`를 사용하려 했으나, 대규모 트래픽과 분산 환경(Sharding)을 고려할 때 다음과 같은 잠재적 문제가 있음.

1.  **Sharding 어려움**: 여러 DB 인스턴스로 데이터를 분산할 때, ID 중복 방지가 어렵거나 복잡한 설정(Offset 조정)이 필요함.
2.  **정보 노출**: ID가 순차적으로 증가하므로, 경쟁사가 하루 거래량 등을 유추하기 쉬움.
3.  **DB 종속성**: Insert가 완료되어야 ID를 알 수 있어, 애플리케이션 레벨에서 ID를 미리 생성하여 처리하는 로직(예: 멱등성 키로 활용)이 불가능함.

## Decision
**TSID (Time-Sorted Unique Identifier)** 를 사용한다.

## Detailed Analysis

### 1. Auto Increment
*   **Pros**: 간편함, 작은 크기(BIGINT), 인덱스 성능 좋음 (순차적).
*   **Cons**: 분산 환경 취약, 채번을 위해 DB Round-trip 필요.

### 2. UUID (v4)
*   **Pros**: 충돌 확률 0에 수렴, DB 독립적 생성 가능.
*   **Cons**: 128-bit로 큼, **무작위성으로 인해 B-Tree 인덱스 파편화(Fragmentation) 발생**, Insert 성능 저하 심각.

### 3. Snowflake / Twitter Snowflake
*   **Pros**: 64-bit(BIGINT), 시간순 정렬, 분산 환경 적합.
*   **Cons**: 별도의 ID 생성 서버(Worker ID 관리)가 필요하거나 주키퍼(Zookeeper) 의존성 발생 가능.

### 4. TSID (Selected)
*   **Pros**:
    *   **64-bit (BIGINT)**: MySQL `BIGINT`에 저장 가능 (공간 효율).
    *   **Time-Sorted**: 시간순으로 정렬되므로 **DB 인덱스 성능(Clustered Index) 최적화**.
    *   **K-Sortable**: UUID와 달리 정렬 가능.
    *   **No Coordination**: 별도 ID 서버 없이 라이브러리 레벨에서 생성 가능 (Node bits 활용).
*   **Cons**: 밀리초 단위 동시 생성 제한(단일 노드 내)이 있으나, 일반적인 금융 트랜잭션 규모에서는 충분함.

## Consequences
*   모든 테이블의 PK는 `BIGINT` 타입을 유지하되, `AUTO_INCREMENT` 속성은 제거한다.
*   애플리케이션(Java) 레벨에서 `Hypersistence TSID` 라이브러리 등을 사용하여 ID를 생성 후 DB에 저장한다.
*   Sharding 시에도 ID 충돌 없이 데이터 분산이 용이해진다.
