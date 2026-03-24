# Inventory Service Guide

## 특수 규칙

- 재고 변경은 `Inventory` 엔티티 메서드로만 수행한다.
- Kafka 소비 결과는 `Inbox`와 `InventoryTransaction`으로 추적 가능해야 한다.
- 재고 부족과 중복 이벤트는 별도 예외 또는 실패 이벤트로 구분한다.

## 테스트 우선 항목

- 재고 예약 성공
- 재고 부족 실패
- 중복 이벤트 무시
- 재고 이력 생성 여부
