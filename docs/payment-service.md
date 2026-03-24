# Payment Service Guide

## 특수 규칙

- 결제 상태의 진실원천은 `Payment` 테이블이다.
- Toss Payments 웹훅은 `중복 확인 -> payment 반영 -> outbox 저장` 순서로 처리한다.
- DB 저장이 끝나기 전에는 외부 PG에 성공 응답을 반환하지 않는다.

## 테스트 우선 항목

- 결제 시작 성공
- 존재하지 않는 paymentId 웹훅 처리
- 중복 웹훅 무시
- outbox 저장 여부
