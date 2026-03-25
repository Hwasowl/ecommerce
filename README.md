# E-commerce

이커머스 환경에서 발생할 수 있는 주문 생성, 외부 결제 연동, 재고 차감, 이벤트 유실 방지 문제를 직접 다루기 위해 만든 프로젝트입니다.  
결제 성공 이후 재고 확보에 실패하는 부분 실패 상황, 메시지 브로커 장애로 인한 후속 처리 지연, 중복 웹훅과 중복 이벤트 소비 같은 문제를 전제로 두고 설계했습니다.  

주문, 결제, 재고를 각각 분리된 서비스로 나누고, `PG 결제(Toss Payments)`, `Kafka`, `Saga`, `Outbox/Inbox`, `Idempotency`를 통해 분산 트랜잭션과 장애 복구 전략을 검증하는 것을 목표로 합니다.

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

PG 웹훅으로 결제 성공을 확정하고, `PaymentConfirmed` 이벤트를 발행한 뒤 Redis 기반 재고 예약과 주문 확정을 진행합니다.

```mermaid
sequenceDiagram
    actor User
    participant OrderService
    participant PaymentService
    participant PG
    participant Kafka
    participant InventoryService
    participant Redis

    User->>OrderService: 1. 주문 생성 요청
    OrderService->>OrderService: 2. 주문 저장 및 상태 이력 저장 (CREATED)
    OrderService-->>User: 3. orderId 반환

    User->>PaymentService: 4. 결제 시작 요청(orderId, items)
    PaymentService->>PaymentService: 5. payment 저장 및 주문 스냅샷 저장 (PENDING)
    PaymentService-->>User: 6. 결제 연동 정보 반환

    PG->>PaymentService: 7. 결제 완료 웹훅
    PaymentService->>PaymentService: 8. 결제 상태를 CONFIRMED로 반영
    PaymentService->>Kafka: 9. PaymentConfirmed 발행

    Kafka->>InventoryService: 10. PaymentConfirmed 전달
    InventoryService->>Redis: 11. 재고 확인 및 차감
    InventoryService->>Kafka: 12. InventoryReserved 발행

    Kafka->>OrderService: 13. InventoryReserved 전달
    OrderService->>OrderService: 14. 주문 상태를 PAID로 반영
```

### 2. 부분 실패 케이스

결제는 성공했지만 재고 확보에 실패한 경우, 보상 트랜잭션으로 환불 또는 취소를 수행합니다.

```mermaid
sequenceDiagram
    actor User
    participant OrderService
    participant PaymentService
    participant PG
    participant Kafka
    participant InventoryService
    participant Redis

    User->>OrderService: 1. 주문 생성
    OrderService-->>User: 2. orderId 반환

    User->>PaymentService: 3. 결제 시작
    PaymentService-->>User: 4. 결제 연동 정보 반환

    PG->>PaymentService: 5. 결제 완료 웹훅
    PaymentService->>PaymentService: 6. 결제 상태를 CONFIRMED로 반영
    PaymentService->>Kafka: 7. PaymentConfirmed 발행

    Kafka->>InventoryService: 8. PaymentConfirmed 전달
    InventoryService->>Redis: 9. 재고 확인 및 차감 시도
    InventoryService->>Kafka: 10. InventoryFailed 발행

    Kafka->>PaymentService: 11. InventoryFailed 전달
    PaymentService->>PG: 12. 결제 취소 또는 환불 요청
    PG-->>PaymentService: 13. 취소 또는 환불 성공
    PaymentService->>PaymentService: 14. 결제 상태를 CANCELED로 반영
    PaymentService->>Kafka: 15. PaymentCompensated 발행

    Kafka->>OrderService: 16. PaymentCompensated 전달
    OrderService->>OrderService: 17. 주문 상태를 FAILED로 반영
```

### 3. 장애 케이스

웹훅을 받아 DB 저장은 끝났지만 Kafka 발행 전에 서버가 내려가는 상황을 가정합니다. 이 문제를 막기 위해 `Outbox`를 둡니다.

```mermaid
sequenceDiagram
    participant PG
    participant PaymentService
    participant PaymentDB
    participant OutboxRelay
    participant Kafka
    participant InventoryService

    PG->>PaymentService: 1. 결제 완료 웹훅
    PaymentService->>PaymentDB: 2. 결제 상태를 CONFIRMED로 저장
    PaymentService->>PaymentDB: 3. PaymentConfirmed를 Outbox에 저장
    PaymentService-->>PG: 4. 200 OK

    Note over PaymentService: 5. Kafka 발행 전 서버 장애 발생
    Note over PaymentDB,OutboxRelay: 6. Outbox Relay가 미전송 이벤트를 조회
    OutboxRelay->>Kafka: 7. PaymentConfirmed 발행
    Kafka->>InventoryService: 8. PaymentConfirmed 전달
    InventoryService->>InventoryService: 9. Inbox로 중복 확인 후 재고 처리
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

## Kafka 및 장애 대응

이 프로젝트는 Kafka가 항상 정상이라는 가정으로 설계하지 않습니다.  
결제 웹훅을 받은 뒤 `어디까지 저장되었는지`, `어디서 장애가 났는지`에 따라 응답 방식과 복구 전략을 다르게 가져갑니다.

### 1. Kafka 장애

Kafka만 죽어 있고 DB는 정상인 경우입니다.  
이때는 결제 상태와 Outbox 이벤트를 DB에 먼저 저장할 수 있으므로, 웹훅에 `200 OK`를 반환해도 유실되지 않습니다. Kafka 복구 후 Outbox Relay가 이벤트를 재발행합니다.

```mermaid
sequenceDiagram
    participant PG
    participant PaymentService
    participant PaymentDB
    participant Kafka
    participant OutboxRelay

    PG->>PaymentService: 1. 결제 완료 웹훅
    PaymentService->>PaymentDB: 2. payment CONFIRMED 저장
    PaymentService->>PaymentDB: 3. PaymentConfirmed Outbox 저장
    PaymentService-->>PG: 4. 200 OK

    OutboxRelay->>Kafka: 5. PaymentConfirmed 발행 시도
    Kafka-->>OutboxRelay: 6. Kafka 장애

    Note over Kafka: 7. Kafka 복구 후
    OutboxRelay->>PaymentDB: 8. 미전송 Outbox 조회
    OutboxRelay->>Kafka: 9. PaymentConfirmed 재발행
```

### 2. DB 장애

Kafka는 살아 있어도 DB가 죽어 있으면 결제 상태를 신뢰성 있게 저장할 수 없습니다.  
이 경우에는 이벤트를 먼저 발행하지 않고, 웹훅에 `5xx`를 반환해서 PG가 동일한 웹훅을 다시 보내도록 유도해야 합니다.

```mermaid
sequenceDiagram
    participant PG
    participant PaymentService
    participant PaymentDB
    participant Kafka

    PG->>PaymentService: 1. 결제 완료 웹훅
    PaymentService->>PaymentDB: 2. payment 저장 시도
    PaymentDB-->>PaymentService: 3. DB 장애

    Note over PaymentService: 4. DB 저장 실패 상태에서는 진행 중단
    Note over PaymentService: 5. Kafka가 살아 있어도 이벤트 선발행 금지

    PaymentService-->>PG: 6. 5xx 응답
    Note over PG: 7. 동일 webhook 재전송
```

### 3. DB + Kafka 전체 장애

DB와 Kafka가 동시에 죽어 있으면 내부에 결제 성공 사실을 남길 수 없습니다.  
이 상황에서는 성공 응답을 주지 않고 `5xx`를 반환한 뒤, 외부 PG의 웹훅 재시도 정책을 최후 안전장치로 사용합니다.

```mermaid
sequenceDiagram
    participant PG
    participant PaymentService
    participant PaymentDB
    participant Kafka

    PG->>PaymentService: 1. 결제 완료 웹훅
    PaymentService->>PaymentDB: 2. payment 저장 시도
    PaymentDB-->>PaymentService: 3. DB 장애
    PaymentService->>Kafka: 4. 이벤트 발행 시도
    Kafka-->>PaymentService: 5. Kafka 장애

    PaymentService-->>PG: 6. 5xx 응답
    Note over PG: 7. PG 재시도 정책에 따라 동일 webhook 재전송
```

복구 이후에는 재전송된 웹훅을 다시 받아서 `payment 저장 -> outbox 저장 -> kafka 발행` 순서로 정상 처리합니다.

```mermaid
sequenceDiagram
    participant PG
    participant PaymentService
    participant PaymentDB
    participant Kafka
    participant OutboxRelay

    Note over PaymentDB,Kafka: 1. DB와 Kafka 복구 완료

    PG->>PaymentService: 2. 동일 웹훅 재전송
    PaymentService->>PaymentDB: 3. payment CONFIRMED 저장
    PaymentService->>PaymentDB: 4. PaymentConfirmed Outbox 저장
    PaymentService-->>PG: 5. 200 OK

    OutboxRelay->>PaymentDB: 6. 미전송 Outbox 조회
    OutboxRelay->>Kafka: 7. PaymentConfirmed 발행
```

### 장애 대응 원칙

- `DB 저장이 끝나기 전에는 200 OK를 반환하지 않는다.`
- `Kafka만 장애인 경우에는 Outbox에 저장되었는지 확인한 뒤 200 OK를 반환한다.`
- `DB가 장애인 경우에는 Kafka가 살아 있어도 이벤트를 먼저 발행하지 않는다.`
- `DB와 Kafka가 모두 장애인 경우에는 PG 웹훅 재시도를 최후 안전장치로 사용한다.`
- `Kafka 소비 측은 Inbox와 eventId 기반 멱등성으로 중복 처리를 막는다.`
