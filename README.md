# Order Payment Inventory MSA

`Java 17 + Spring Boot 3 + Spring Data JPA` 기준의 주문, 결제, 재고 서비스 골격입니다.  
외부 결제는 `Toss Payments`, 서비스 간 비동기 연동은 `Kafka`, 데이터 정합성은 `Saga + Outbox/Inbox + Idempotency`를 전제로 설계합니다.

## 구성

- `order-service`
  - 주문, 주문상품, 주문상태이력 엔티티
- `payment-service`
  - Toss Payments 기준 결제, 웹훅, 보상, 아웃박스 엔티티
  - `/api/v1/toss/webhooks/payments` 웹훅 엔드포인트
- `inventory-service`
  - 재고, 재고예약, 재고이력, 인박스 엔티티

## 아키텍처

아래 구조를 기준으로 주문, 결제, 재고를 분리합니다.

```mermaid
flowchart LR
    Client[Client]
    Order[Order Service]
    Payment[Payment Service]
    Inventory[Inventory Service]
    Kafka[Kafka]
    Toss[Toss Payments]

    Client -->|주문 생성| Order
    Client -->|결제 시작| Payment
    Payment -->|결제 승인 요청| Toss
    Toss -->|Webhook| Payment

    Payment -->|PaymentConfirmed PaymentFailed| Kafka
    Kafka -->|Consume| Inventory
    Kafka -->|Consume| Order

    Inventory -->|InventoryReserved InventoryFailed| Kafka
    Kafka -->|Consume| Order
    Kafka -->|Consume| Payment
```

## 시퀀스 다이어그램

### 1. 성공 케이스

Toss Payments 웹훅으로 결제 성공을 확정하고, `PaymentConfirmed` 이벤트를 발행한 뒤 재고 차감과 주문 확정을 진행합니다.

```mermaid
sequenceDiagram
    actor User
    participant OrderService
    participant PaymentService
    participant TossPayments
    participant IdempotencyStore
    participant Kafka
    participant InventoryService

    User->>OrderService: 주문 생성 요청
    OrderService->>OrderService: 주문 저장 (CREATED)
    OrderService-->>User: orderId 반환

    User->>PaymentService: 결제 시작 요청(orderId)
    PaymentService->>PaymentService: payment 저장 (PENDING)
    PaymentService->>TossPayments: 결제 요청
    TossPayments-->>User: 결제 진행

    TossPayments->>PaymentService: webhook(eventId, orderId, paymentId, paymentKey, status=SUCCESS)
    PaymentService->>IdempotencyStore: eventId 확인
    IdempotencyStore-->>PaymentService: not found
    PaymentService->>IdempotencyStore: eventId 저장 (PROCESSING)
    PaymentService->>PaymentService: payment 상태 변경 (CONFIRMED)
    PaymentService->>Kafka: PaymentConfirmed 발행
    PaymentService->>IdempotencyStore: eventId 상태 변경 (COMPLETED)
    PaymentService-->>TossPayments: 200 OK

    Kafka->>InventoryService: PaymentConfirmed 전달
    InventoryService->>InventoryService: 멱등성 확인
    InventoryService->>InventoryService: 재고 차감
    InventoryService->>Kafka: InventoryReserved 발행

    Kafka->>OrderService: InventoryReserved 전달
    OrderService->>OrderService: 주문 상태 변경 (PAID)
```

### 2. 부분 실패 케이스

결제는 성공했지만 재고 확보에 실패한 경우, 보상 트랜잭션으로 환불 또는 취소를 수행합니다.

```mermaid
sequenceDiagram
    participant PaymentService
    participant TossPayments
    participant IdempotencyStore
    participant Kafka
    participant InventoryService
    participant CompensationService
    participant OrderService

    TossPayments->>PaymentService: webhook(eventId, orderId, paymentId, paymentKey, status=SUCCESS)
    PaymentService->>IdempotencyStore: eventId 확인
    IdempotencyStore-->>PaymentService: not found
    PaymentService->>IdempotencyStore: eventId 저장 (PROCESSING)
    PaymentService->>PaymentService: payment 상태 변경 (CONFIRMED)
    PaymentService->>Kafka: PaymentConfirmed 발행
    PaymentService->>IdempotencyStore: eventId 상태 변경 (COMPLETED)
    PaymentService-->>TossPayments: 200 OK

    Kafka->>InventoryService: PaymentConfirmed 전달
    InventoryService->>InventoryService: 재고 차감 시도
    InventoryService->>Kafka: InventoryFailed 발행

    Kafka->>CompensationService: InventoryFailed 전달
    CompensationService->>TossPayments: 결제 취소 또는 환불 요청
    TossPayments-->>CompensationService: 취소 성공
    CompensationService->>Kafka: PaymentCompensated 발행

    Kafka->>OrderService: PaymentCompensated 전달
    OrderService->>OrderService: 주문 상태 변경 (FAILED)
```

### 3. 장애 케이스

웹훅을 받아 DB 저장은 끝났지만 Kafka 발행 전에 서버가 내려가는 상황을 가정합니다. 이 문제를 막기 위해 `Outbox`를 둡니다.

```mermaid
sequenceDiagram
    participant TossPayments
    participant PaymentService
    participant IdempotencyStore
    participant Outbox
    participant Kafka
    participant InventoryService

    TossPayments->>PaymentService: webhook(eventId, orderId, paymentId, paymentKey, status=SUCCESS)
    PaymentService->>IdempotencyStore: eventId 확인
    IdempotencyStore-->>PaymentService: not found
    PaymentService->>IdempotencyStore: eventId 저장 (PROCESSING)
    PaymentService->>PaymentService: payment 상태 변경 (CONFIRMED)
    PaymentService->>Outbox: PaymentConfirmed 이벤트 저장

    Note over PaymentService: Kafka 발행 전 서버 장애 발생

    TossPayments->>PaymentService: 동일 webhook 재전송
    PaymentService->>IdempotencyStore: eventId 재확인
    IdempotencyStore-->>PaymentService: PROCESSING 또는 COMPLETED 존재
    PaymentService-->>TossPayments: 200 OK

    Note over Outbox,Kafka: Outbox relay가 미전송 이벤트 복구

    Outbox->>Kafka: PaymentConfirmed 발행
    Kafka->>InventoryService: PaymentConfirmed 전달
    InventoryService->>InventoryService: 멱등성 확인 후 재고 차감
```

## ERD

서비스 간 FK는 실제 DB FK로 연결하지 않고, `order_id`, `payment_id`, `event_id` 같은 식별자로만 연결합니다.

### Order Service

```mermaid
erDiagram
    ORDERS {
        bigint id PK
        varchar order_id UK
        bigint customer_id
        varchar order_status
        varchar currency
        decimal total_amount
        datetime created_at
        datetime updated_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_pk FK
        bigint product_id
        varchar product_name
        int quantity
        decimal unit_price
        decimal line_amount
    }

    ORDER_STATUS_HISTORY {
        bigint id PK
        varchar order_id
        varchar from_status
        varchar to_status
        varchar reason
        varchar event_id
        datetime changed_at
    }

    ORDERS ||--o{ ORDER_ITEMS : contains
    ORDERS ||--o{ ORDER_STATUS_HISTORY : tracks
```

### Payment Service

```mermaid
erDiagram
    PAYMENTS {
        bigint id PK
        varchar payment_id UK
        varchar order_id
        varchar pg_provider
        varchar payment_key
        varchar payment_status
        decimal amount
        varchar currency
        varchar pg_transaction_id
        datetime approved_at
        datetime canceled_at
        datetime created_at
        datetime updated_at
    }

    PAYMENT_WEBHOOKS {
        bigint id PK
        varchar event_id UK
        varchar payment_id
        varchar order_id
        varchar payment_key
        varchar webhook_type
        varchar webhook_status
        text payload_json
        varchar signature
        varchar process_status
        datetime received_at
        datetime processed_at
    }

    PAYMENT_COMPENSATIONS {
        bigint id PK
        varchar compensation_id UK
        varchar payment_id
        varchar order_id
        varchar compensation_type
        varchar compensation_status
        decimal amount
        varchar reason
        datetime requested_at
        datetime completed_at
    }

    PAYMENT_OUTBOX {
        bigint id PK
        varchar event_id UK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        text payload_json
        varchar publish_status
        datetime created_at
        datetime published_at
    }
```

### Inventory Service

```mermaid
erDiagram
    INVENTORIES {
        bigint id PK
        bigint product_id UK
        int total_stock
        int reserved_stock
        int available_stock
        datetime created_at
        datetime updated_at
    }

    INVENTORY_RESERVATIONS {
        bigint id PK
        varchar reservation_id UK
        varchar order_id
        varchar payment_id
        bigint product_id
        int quantity
        varchar reservation_status
        datetime reserved_at
        datetime released_at
    }

    INVENTORY_TRANSACTIONS {
        bigint id PK
        varchar transaction_id UK
        bigint product_id
        varchar order_id
        varchar payment_id
        varchar transaction_type
        int quantity
        int before_stock
        int after_stock
        varchar reason
        datetime created_at
    }

    INVENTORY_INBOX {
        bigint id PK
        varchar event_id UK
        varchar event_type
        varchar aggregate_id
        text payload_json
        varchar process_status
        datetime received_at
        datetime processed_at
    }
```

## 실행

프로젝트 루트의 Gradle Wrapper를 사용합니다.

```bash
./gradlew :order-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :inventory-service:bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat :order-service:bootRun
.\gradlew.bat :payment-service:bootRun
.\gradlew.bat :inventory-service:bootRun
```

## 현재 구현 범위

- 멀티모듈 Gradle 구조
- 서비스별 Spring Boot 3 설정
- 서비스별 JPA 엔티티 및 Repository
- Toss Payments 웹훅 엔드포인트의 최소 골격

## 다음 구현 후보

- 주문 생성 API와 상태 전이 서비스
- Toss 결제 승인 API 연동 클라이언트
- Kafka 이벤트 발행과 인박스/아웃박스 릴레이
- 보상 트랜잭션 서비스와 테스트
