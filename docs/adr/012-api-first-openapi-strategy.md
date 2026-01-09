# [ADR-012] API-First 접근 방식과 OpenAPI Spec 전략

*   **Status**: Accepted
*   **Date**: 2026-01-09
*   **Author**: dding94

## 1. Context (배경)

### 1.1. 문제 상황

Order System의 REST API를 개발하면서 API 문서화 방식을 결정해야 합니다.

```
┌─────────────────────────────────────────────────────────────┐
│  OrderController                                            │
│  ├─ POST /api/v1/orders          ← 주문 생성               │
│  ├─ GET  /api/v1/orders/{orderId} ← 주문 조회              │
│  └─ POST /api/v1/orders/{orderId}/cancel ← 주문 취소       │
└─────────────────────────────────────────────────────────────┘
          ↓
   API 문서화 방식 결정 필요
          ↓
   ┌─────────────────┬─────────────────┐
   │   Code-First    │   Spec-First    │
   │ (어노테이션 기반)│  (API-First)     │
   └─────────────────┴─────────────────┘
```

**핵심 질문:**
- API 명세는 코드에서 **자동 생성**되어야 하는가?
- 아니면 명세가 **Single Source of Truth(SSOT)**로서 코드보다 **선행**해야 하는가?

### 1.2. 현실적 고려 사항

| 상황 | 설명 |
|------|------|
| **팀 구성** | 현재는 백엔드 단독 개발, 향후 프론트엔드 개발자 합류 예정 |
| **API 성격** | 금융 주문 API - 명세 변경 시 파급 효과가 큼 |
| **개발 단계** | 초기 설계 단계 - API 계약을 명확히 정의할 필요 있음 |
| **확장성** | 향후 모바일 앱, 외부 파트너 API 연동 가능성 |

### 1.3. 왜 API 설계 방식이 중요한가?

**금융 시스템에서 API는 "계약"이다:**

```
┌─────────────────────────────────────────────────────────────┐
│  API 변경 = 기존 클라이언트 영향                            │
│                                                             │
│  - 주문 응답에서 필드명 변경?                               │
│    → 프론트엔드 파싱 오류 → 주문 실패                       │
│                                                             │
│  - 에러 코드 체계 변경?                                     │
│    → 클라이언트 에러 핸들링 실패 → 사용자 혼란              │
│                                                             │
│  - 필수 필드 추가?                                          │
│    → 기존 클라이언트 요청 실패 → 서비스 장애                │
└─────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> API 설계는 **"한번 공개되면 쉽게 변경할 수 없다"**는 전제 하에 신중하게 결정되어야 합니다.

## 2. Decision (결정)

**API-First(Spec-First) 접근 방식**을 채택하여 OpenAPI YAML 스펙을 먼저 작성하고, 이를 기반으로 구현합니다.

### 핵심 원칙

> **"API 명세(YAML)가 설계의 원천(Source of Truth)이다. 코드는 명세를 구현하는 것이다."**

```
┌─────────────────────────────────────────────────────────────┐
│  API-First Workflow                                          │
│                                                             │
│  1. order-api.yaml 작성 (설계자/아키텍트)                   │
│     ↓                                                       │
│  2. 팀 리뷰 및 피드백 (Breaking Change 사전 검토)           │
│     ↓                                                       │
│  3. YAML 확정 → Git Commit                                  │
│     ↓                                                       │
│  4. Controller 구현 (YAML 명세 준수)                        │
│     ↓                                                       │
│  5. Swagger UI에서 YAML 직접 렌더링                         │
└─────────────────────────────────────────────────────────────┘
```

### 구현 구조

```
kuku-order-system/
└── src/main/resources/
    └── openapi/
        └── order-api.yaml    ← 275 라인, 모든 API 명세 중앙 관리

application.yml:
  springdoc:
    swagger-ui:
      url: /openapi/order-api.yaml  ← 정적 파일 직접 로드
```

**Controller는 비즈니스 로직에만 집중:**

```java
// OrderController.java - OpenAPI 어노테이션 없음 (Clean Code)
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        // 순수 비즈니스 로직만 존재
        // @Operation, @ApiResponse 어노테이션 없음
    }
}
```

## 3. Alternatives Considered (대안 분석)

### 3.1. Spec-First (API-First) ✅ (선택)

OpenAPI YAML 파일을 먼저 작성하고 SpringDoc으로 렌더링:

```yaml
# order-api.yaml
openapi: 3.0.3
info:
  title: Kuku Order System API
  version: v1
paths:
  /api/v1/orders:
    post:
      operationId: placeOrder
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/PlaceOrderRequest"
```

| 장점 | 단점 |
|------|------|
| API 설계를 먼저 고민하게 함 | 코드와 명세 간 동기화 수동 필요 |
| YAML이 계약서 역할 (SSOT) | 초기 작성 비용 |
| Git으로 명세 변경 이력 추적 용이 | 개발자가 YAML 문법 학습 필요 |
| 프론트엔드 병렬 개발 가능 | |
| `openapi-generator`로 클라이언트 SDK 생성 가능 | |

### 3.2. Code-First (어노테이션 기반) ❌

Controller에 `@Operation`, `@ApiResponse` 어노테이션을 추가하여 런타임에 명세 생성:

```java
// Code-First 방식 예시
@Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "주문 생성 성공"),
    @ApiResponse(responseCode = "422", description = "검증 실패")
})
@PostMapping
public ResponseEntity<OrderResponse> placeOrder(...) { ... }
```

| 장점 | 단점 |
|------|------|
| 코드와 문서가 항상 동기화 | 어노테이션으로 Controller 비대화 |
| 런타임 자동 생성 | API 변경 시 설계 고민 없이 즉흥적 변경 위험 |
| 빠른 초기 개발 | 명세 변경 이력 추적 어려움 (코드 diff에 묻힘) |
| | 프론트엔드는 구현 완료 후에야 API 확인 가능 |

> [!WARNING]
> **왜 Code-First를 선택하지 않았는가?**
> 1. **Controller 오염**: 비즈니스 로직과 문서화 어노테이션이 혼재
> 2. **설계 경시**: "일단 구현하고 문서는 나중에" 유혹 → API 품질 저하
> 3. **계약 불명확**: API 변경이 Git 히스토리에서 명확히 드러나지 않음

### 3.3. Hybrid (Code-First + 수동 YAML 병행) ❌

두 방식을 혼합하여 어노테이션으로 기본 문서 생성 후 YAML 수동 보강:

| 장점 | 단점 |
|------|------|
| 유연함 | **두 군데 관리 = 동기화 실패 위험** |
| 점진적 도입 가능 | 어떤 것이 정본(Truth)인지 혼란 |
| | 유지보수 비용 증가 |

> **"하나의 원천만 관리하라"** 원칙 위배

### 3.4. Design-First with Code Generation ❌ (현 단계에서)

OpenAPI YAML에서 Controller 인터페이스까지 자동 생성:

```xml
<!-- openapi-generator-maven-plugin -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/openapi/order-api.yaml</inputSpec>
        <generatorName>spring</generatorName>
    </configuration>
</plugin>
```

| 장점 | 단점 |
|------|------|
| 명세-코드 완벽 동기화 | 생성된 코드 커스터마이징 어려움 |
| 컴파일 타임 명세 검증 | 빌드 복잡도 증가 |
| | 초기 학습 곡선 높음 |

> [!NOTE]
> **향후 고려**: Week 9 이후 클라이언트 SDK 생성 필요 시 부분 도입 검토
> - 서버 코드 생성: 유연성 저하로 미채택
> - 클라이언트 SDK 생성: 채택 고려 (TypeScript, Kotlin 등)

## 4. Trade-off Analysis (상세 비교 분석)

### 4.1. 설계 품질 관점

```
┌────────────────────────────────────────────────────────────────┐
│  Code-First의 위험성                                            │
│                                                                │
│  [개발자 A]                                                    │
│  "OrderResponse에 'internalNote' 필드 추가할게요"              │
│               ↓                                                │
│  코드 수정 → 어노테이션 추가 → PR 머지 → API 변경 완료         │
│               ↓                                                │
│  [프론트엔드 개발자]                                           │
│  "갑자기 새 필드가 생겼네요? 왜요?"                            │
│               ↓                                                │
│  Breaking Change 발생!                                         │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│  Spec-First의 안전망                                            │
│                                                                │
│  [개발자 A]                                                    │
│  "OrderResponse에 'internalNote' 필드 추가하려면..."           │
│               ↓                                                │
│  1. order-api.yaml 수정 (변경점 명확히 드러남)                 │
│  2. PR에서 YAML diff 확인 → "왜 이 필드가 필요한가?" 논의      │
│  3. 팀 합의 후 머지                                            │
│               ↓                                                │
│  Breaking Change 사전 방지                                     │
└────────────────────────────────────────────────────────────────┘
```

### 4.2. 협업 관점

| 시나리오 | Code-First | Spec-First |
|----------|------------|------------|
| 프론트엔드 개발 시작 시점 | 백엔드 구현 완료 후 | YAML 확정 즉시 |
| API 변경 제안 | 코드 PR + 설명 | YAML PR (변경점 명확) |
| 외부 파트너 API 문서 제공 | Swagger UI 링크 | YAML 파일 직접 제공 가능 |
| API 버전 관리 | 복잡 (코드 분기) | YAML 파일 버전별 관리 |

### 4.3. 확장성 관점

```
┌─────────────────────────────────────────────────────────────┐
│  Spec-First의 확장 가능성                                   │
│                                                             │
│  order-api.yaml                                             │
│       ↓                                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ openapi-generator                                    │   │
│  │   ├─ TypeScript Client (프론트엔드)                  │   │
│  │   ├─ Kotlin Client (안드로이드 앱)                   │   │
│  │   ├─ Swift Client (iOS 앱)                           │   │
│  │   └─ Python Client (데이터 분석팀)                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  하나의 YAML로 모든 클라이언트 SDK 자동 생성 가능!         │
└─────────────────────────────────────────────────────────────┘
```

## 5. Implementation Details (구현 상세)

### 5.1. OpenAPI YAML 구조

```yaml
# 파일 위치: /src/main/resources/openapi/order-api.yaml
openapi: 3.0.3
info:
  title: Kuku Order System API
  version: v1
  description: |
    주문 생성, 조회, 취소 API - Kuku Securities

    ## 주문 상태 흐름
    CREATED → VALIDATED → FILLED
                      ↘ REJECTED
                      ↘ CANCELLED

servers:
  - url: http://localhost:8082
    description: Local Development Server

tags:
  - name: Orders
    description: 주문 관리 API

paths:
  /api/v1/orders:
    post:
      tags: [Orders]
      summary: 주문 생성
      operationId: placeOrder
      # ... 상세 정의

components:
  schemas:
    PlaceOrderRequest:
      type: object
      required: [accountId, symbol, quantity, side, orderType]
      properties:
        accountId:
          type: integer
          format: int64
        # ...
```

### 5.2. SpringDoc 설정

```yaml
# application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    url: /openapi/order-api.yaml    # 핵심: 정적 YAML 직접 로드
    tags-sorter: alpha
    operations-sorter: method
  api-docs:
    path: /v3/api-docs
```

> [!NOTE]
> `swagger-ui.url`을 사용하면 어노테이션 스캔 없이 YAML 파일을 직접 렌더링합니다.
> Controller의 어노테이션을 스캔하는 기본 동작을 **비활성화**하는 효과가 있습니다.

### 5.3. 코드-명세 동기화 검증 (CI 연동)

**문제**: YAML 명세와 실제 Controller 구현이 일치하지 않을 위험

**해결책**: 통합 테스트에서 응답 구조 검증

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrderApiContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void placeOrder_responseMatchesOpenApiSpec() {
        // Given
        PlaceOrderRequest request = createValidRequest();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate
            .postForEntity("/api/v1/orders", request, OrderResponse.class);
        
        // Then - YAML 스펙에 정의된 필드 존재 확인
        assertThat(response.getBody().getOrderId()).isNotNull();
        assertThat(response.getBody().getStatus()).isIn("CREATED", "VALIDATED", "REJECTED");
        // ... 모든 필수 필드 검증
    }
}
```

**향후 개선 (Week 9+)**: `openapi-diff` 도구로 YAML 변경 시 Breaking Change 자동 검출

```bash
# CI Pipeline에 추가
npx openapi-diff ./order-api.yaml.old ./order-api.yaml.new --fail-on-breaking
```

## 6. Consequences (결과)

### 6.1. 기대 효과

| 측면 | 효과 |
|------|------|
| **설계 품질** | API 변경 시 YAML에서 명확한 diff 확인 → 신중한 결정 |
| **협업 효율** | 프론트엔드가 백엔드 구현 전에 Mock 서버 운영 가능 |
| **코드 청결** | Controller에서 어노테이션 제거 → 비즈니스 로직 집중 |
| **확장성** | 클라이언트 SDK 자동 생성 토대 마련 |
| **추적성** | Git에서 API 변경 이력만 별도 추적 가능 |

### 6.2. 트레이드오프

| 비용 | 설명 | 완화 방안 |
|------|------|-----------|
| **수동 동기화** | YAML과 코드가 불일치할 위험 | 통합 테스트로 검증, 향후 CI에 스키마 비교 도구 추가 |
| **학습 비용** | 개발자가 OpenAPI 3.0 문법 학습 필요 | 템플릿 제공, 팀 가이드 문서화 |
| **초기 작성 비용** | 구현 전 YAML 작성 시간 필요 | 장기적으로 재작업 감소로 상쇄 |

### 6.3. 모니터링 및 운영

**Swagger UI 접속 확인:**
```
http://localhost:8082/swagger-ui.html
```

**YAML 변경 시 체크리스트:**
1. [ ] version 필드 업데이트 (SemVer 준수)
2. [ ] Breaking Change 여부 확인 (필드 삭제, 타입 변경)
3. [ ] 팀원 리뷰 완료
4. [ ] 통합 테스트 통과 확인

## 7. Comparison Summary (최종 비교표)

| 평가 기준 | Code-First | Spec-First (선택) | 승자 |
|-----------|------------|-------------------|------|
| **설계 우선순위** | 구현 후 문서화 | 명세 먼저 설계 | ⭐ Spec-First |
| **Controller 청결도** | 어노테이션 분산 | 어노테이션 없음 | ⭐ Spec-First |
| **명세 변경 추적** | 코드 diff에 묻힘 | YAML diff 명확 | ⭐ Spec-First |
| **프론트엔드 협업** | 구현 완료 후 확인 | 설계 단계부터 공유 | ⭐ Spec-First |
| **클라이언트 SDK 생성** | 불편 | `openapi-generator` 활용 용이 | ⭐ Spec-First |
| **초기 개발 속도** | 빠름 | YAML 작성 비용 | ⭐ Code-First |
| **자동 동기화** | 코드 변경 시 자동 반영 | 수동 동기화 필요 | ⭐ Code-First |

**결론**: 6:2로 **Spec-First 방식**이 본 프로젝트에 더 적합

## 8. Interview Q&A (면접 예상 질문)

### Q1. "왜 SpringDoc의 어노테이션 기반 문서화를 사용하지 않았나요?"

**A**: SpringDoc의 어노테이션 기반 문서화는 빠른 프로토타이핑에는 좋지만, 금융 시스템의 API 계약 관리에는 부적합합니다.

1. **Controller 오염**: `@Operation`, `@ApiResponse` 등 어노테이션이 비즈니스 로직과 섞여 가독성이 저하됩니다.
2. **설계 경시**: "일단 코드 작성 후 문서화"라는 접근은 API 설계를 소홀히 하게 만듭니다.
3. **변경 추적 어려움**: API 스펙 변경이 코드 변경 속에 묻혀 Breaking Change를 놓칠 수 있습니다.

Spec-First 방식을 통해 API 설계를 먼저 확정하고, YAML 변경 이력을 Git에서 명확히 추적할 수 있습니다.

### Q2. "YAML 파일과 실제 구현이 불일치하면 어떻게 감지하나요?"

**A**: 현재는 통합 테스트에서 응답 구조를 검증합니다. 향후 CI 파이프라인에 `openapi-diff` 도구를 추가하여 자동 검출할 계획입니다.

```bash
# Breaking Change 자동 감지
npx openapi-diff ./main.yaml ./pr.yaml --fail-on-breaking
```

또한 Week 9에서 `openapi-generator`로 클라이언트 인터페이스를 생성하면, 컴파일 타입에 불일치를 잡을 수 있습니다.

### Q3. "Code Generation을 안 쓰면 Spec-First의 이점이 반감되지 않나요?"

**A**: 부분적으로 동의합니다. 하지만 Code Generation은 다음 단점이 있습니다:

1. **생성 코드 커스터마이징 어려움**: Validation, Exception Handling 등 세밀한 제어 필요
2. **빌드 복잡도 증가**: Gradle 플러그인 설정, 생성 코드 관리 부담

현재 프로젝트 규모에서는 **YAML 중앙 관리 + 수동 구현**이 더 효율적입니다. 향후 API가 20개 이상으로 확장되면 Code Generation 도입을 재검토할 예정입니다.

### Q4. "왜 Swagger Codegen 대신 openapi-generator를 언급하나요?"

**A**: `swagger-codegen`은 SmartBear 사가 관리하지만 개발이 정체되어 있습니다. `openapi-generator`는 커뮤니티 포크로 더 활발하게 유지보수되고, 더 많은 언어/프레임워크를 지원합니다.

## 9. Future Considerations (향후 고려사항)

### 9.1. 클라이언트 SDK 생성 (Week 9+)

```bash
# TypeScript 클라이언트 생성
openapi-generator generate -i order-api.yaml -g typescript-axios -o ./generated/ts-client
```

### 9.2. API 버저닝 전략

```yaml
# 향후 버전 분리
/openapi/
├── order-api-v1.yaml  # 현재 버전
└── order-api-v2.yaml  # 다음 메이저 버전
```

### 9.3. 통합 Mono-repo YAML 관리

```yaml
# 여러 모듈의 YAML 통합 관리
/specs/
├── order-api.yaml
├── ledger-api.yaml
└── market-data-api.yaml
```

## 10. References

*   [OpenAPI Specification 3.0.3](https://spec.openapis.org/oas/v3.0.3)
*   [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
*   [springdoc-openapi Official Guide](https://springdoc.org/)
*   [OpenAPI Generator](https://openapi-generator.tech/)
*   [API Design First - SmartBear](https://smartbear.com/learn/api-design/what-is-api-design-first/)
*   Martin Fowler - [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
*   `/docs/strategy/week5-strategy.md` - PR 4 상세 내용
*   `/kuku-order-system/src/main/resources/openapi/order-api.yaml` - 실제 구현된 Spec
