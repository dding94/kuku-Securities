# Kuku Securities (쿠쿠증권) 📈

> **"Why"가 이끄는 기술적 의사결정, 그리고 극한의 안정성**
> MSA 기반의 고성능 증권 트레이딩 플랫폼 프로젝트입니다.

## 🏗 System Architecture (MSA)

이 프로젝트는 도메인별로 독립적인 마이크로서비스로 구성되어 있습니다.

```mermaid
graph TD
    %% 외부 사용자 및 시스템
    User["📱 Client (App/Web)"]
    ExternalEx["🏦 External Exchanges<br/>(Upbit, Binance)"]

    %% AWS Cloud Boundary
    subgraph AWS_Cloud [" AWS Cloud (VPC) "]
        
        %% Load Balancer & Ingress
        ALB[" Application Load Balancer"]
        
        %% Public Subnet / DMZ
        subgraph Public_Subnet
            Gateway[" API Gateway<br/>(Spring Cloud Gateway)"]
        end

        %% Private Subnet / EKS Cluster
        subgraph Private_Subnet_EKS [" Kubernetes Cluster (EKS) "]
            
            %% Service Group: Trading Domain
            subgraph Domain_Services [Trading Core Domain]
                Order[" Order Service<br/>(주문 접수/상태관리)"]
                Matching["⚙️ Matching Engine<br/>(모의 체결 시뮬레이션)"]
                Ledger["💰 Core Ledger Service<br/>(원장/이중부기)"]
                Position["📊 Portfolio/Position Service<br/>(잔고/수익률 Projection)"]
            end
            
            %% Service Group: Data & Support
            subgraph Support_Services [Data & Support]
                MarketData["📈 Market Data Service<br/>(외부 시세 수집/가공)"]
                SocketServer["🔌 Real-time Push Server<br/>(Netty/WebFlux)"]
                Reference["🗂️ Reference Service<br/>(종목/기준정보)"]
            end
        end

        %% Managed Services (Data & Event)
        subgraph Data_Layer [Persistence & Messaging]
            Kafka["📨 Amazon MSK (Kafka)<br/>(Event Backbone)"]
            Redis["⚡ Amazon ElastiCache (Redis)<br/>(Cache & Distributed Lock)"]
            RDS["💽 Amazon Aurora (MySQL)<br/>(Main Database)"]
            ES["🔍 OpenSearch/ELK<br/>(Logs & Monitoring)"]
        end
    end

    %% Flow Connections
    User -->|HTTPS| ALB
    ALB --> Gateway
    ExternalEx -->|WebSocket/REST| MarketData

    %% Gateway Routing
    Gateway --> Order
    Gateway --> Ledger
    Gateway --> Position
    Gateway --> Reference
    Gateway -->|WebSocket Upgrade| SocketServer

    %% Event Driven Flow (Trading)
    Order -- "OrderPlacedEvent" --> Kafka
    Kafka -- Consume --> Matching
    Matching -- "TradeMatchEvent" --> Kafka
    Kafka -- Consume --> Ledger
    Ledger -- "BalanceUpdatedEvent" --> Kafka
    Kafka -- Consume --> Position

    %% Real-time Data Flow
    MarketData -- "QuoteEvent" --> Kafka
    Kafka -- Consume --> SocketServer
    SocketServer -- "Push" --> User
    MarketData -.->|Save| Redis

    %% Database Connections
    Ledger -.-> RDS
    Order -.-> RDS
    Position -.-> RDS
    Reference -.-> RDS
    
    %% Cache Connections
    Order -.->|Dist. Lock| Redis
    Position -.->|Cache| Redis
```

| Module | Description | Port |
|--------|-------------|------|
| **[kuku-core-ledger](kuku-core-ledger/README.md)** | 원장 시스템 (계좌, 자산, 이중부기) | 8081 |
| **kuku-order-system** | 주문 시스템 (매수/매도, 동시성 제어) | 8082 |
| **kuku-market-data** | 시세 시스템 (실시간 시세, WebSocket) | 8083 |
| **kuku-api-gateway** | API 게이트웨이 (인증, 라우팅) | 8080 |
| **kuku-common** | 공통 유틸리티 및 도메인 객체 | - |

## 🛠 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.0
- **Database**: MySQL 8.0, Redis
- **Messaging**: Kafka
- **Build Tool**: Gradle (Multi-module)

## 🚀 Getting Started

### Prerequisites
- JDK 21
- Docker & Docker Compose

### Run Locally
```bash
# Start Infrastructure (MySQL, Redis, Kafka)
docker-compose up -d

# Build Project
./gradlew clean build
```
