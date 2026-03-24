# Coding Conventions

## 목적

이 문서는 `order-service`, `payment-service`, `inventory-service` 전반에 공통으로 적용할 개발 규칙을 정리한다.

## 패키지 구조

- 패키지는 `api`, `application`, `domain`, `repository`, `exception`, `config`, `infrastructure` 순서로 분리한다.
- HTTP 요청과 응답은 `api` 계층에서만 다룬다.
- 비즈니스 로직은 `application` 계층에 둔다.
- 상태와 행위는 `domain` 엔티티 또는 도메인 객체로 캡슐화한다.
- 외부 시스템 연동 코드는 `infrastructure`에 둔다.

## 계층별 책임

- Controller는 요청 검증, 응답 반환, 상태코드 결정만 담당한다.
- Service는 유스케이스 단위의 비즈니스 흐름을 담당한다.
- Entity는 상태 변경 규칙과 도메인 행위를 가진다.
- Repository는 영속성 접근만 담당하고 비즈니스 규칙을 담지 않는다.

## 식별자 규칙

- 서비스 간 연계는 DB FK 대신 `orderId`, `paymentId`, `reservationId`, `eventId` 같은 비즈니스 식별자를 사용한다.
- 외부 결제 식별자와 내부 식별자를 혼용하지 않는다.
- 상태값은 enum으로 관리하고 문자열 하드코딩을 피한다.

## 테스트 작성 규칙

- 테스트는 `Controller 단위 테스트`, `Service 통합 테스트`, `Repository 테스트`로 역할을 분리한다.
- Controller 테스트는 `@WebMvcTest` 기반으로 요청/응답, validation, 상태코드, 예외 응답을 검증한다.
- Service 테스트는 `@SpringBootTest` 기반으로 실제 DB와 트랜잭션을 포함한 비즈니스 흐름을 검증한다.
- Repository 테스트는 `@DataJpaTest` 기반으로 매핑과 조회 메서드를 검증한다.
- 문서 노출 여부처럼 유지보수 가치가 낮은 테스트는 기본적으로 만들지 않는다.
- 성공 케이스를 작성했다면 대응되는 실패 케이스도 함께 작성한다.
- 실패 케이스는 validation 실패, 리소스 없음, 중복 처리, 상태 충돌 같은 예외 흐름을 포함한다.
- 외부 API 연동은 실제 호출보다 애플리케이션 레벨 테스트와 계약 중심 검증을 우선한다.
- 테스트 메서드명은 카멜케이스로 작성하고, 의미가 드러나도록 `createOrderAndPersistItemsAndStatusHistory`, `failWebhookHandlingWhenPaymentDoesNotExist` 같은 형태를 사용한다.
- 모든 테스트에는 `@DisplayName`을 작성하고, 비즈니스 의도가 드러나는 문장형 설명을 사용한다.

## 상태 변경 규칙

- 상태 전이는 엔티티 메서드로 캡슐화한다.
- 서비스에서 상태 필드를 직접 대입하는 방식은 지양한다.
- 상태 이력이 필요한 경우 별도 이력 테이블 또는 이벤트 로그를 남긴다.
