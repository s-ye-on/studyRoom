# 프로젝트 전체 목표 
## 핵심 포인트
1. 동시성 처리
2. Clock 도입
3. 정책 객체 설계
4. WAIT_PAYMENT 결제 흐름
5. Controller Slice Test (Controller단 테스트)
6. ErrorResponse 통합 예외 처리 
7. DB 인텍스 + EXPLAIN

---
## 1️⃣ Clock 도입 
이유 : `LocalDateTime.now()` 테스트 불가능
```java
@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
```
서비스에서
```java
LocalDateTime.now(clock)
```
테스트에서는 
```java
@MockitoBean Clock clock;
Mockito.when(clock.instant()).thenReturn(fixedInstant);
```
---
## 2️⃣ Controller Slice Test
👉 `@WebMvcTest`
왜?  
- 컨트롤러만 테스트
- Service는 mock  

학습 포인트
- 인증 principal 테스트
- ErrorResponse 검증
- JSON request/response

---
## 3️⃣ 동시성 테스트
👉CountDownLatch + ExecutorService  

목표  
- 동시에 예약 -> DB에는 1개만  

배운 것
- 비관적 락 사용
- `@Transactioanl` 테스트와 스레드 관계 
- ThreadLocal 트랜잭션 개념 (자바는 한 트랜잭션에 한 스레드만)  

---
## 4️⃣ ReservationPolicy 구조 만들었음
### 인터페이스 
```java
public interface ReservationPolicy {
    PolicyPhase phase();
    void validate(...);
}
```
### 구현체 
- OperatingTimePolicy
- MinimumDurationPolicy
- PaymentPolicy  
👉단계별 정책 구조 완성  

---
## 5️⃣ 예약 상태 설계 
### 상태 
```java
WAIT_PAYMENT
CONFIRMED
CANCELED
EXPIRED
```
### 철학  
👉 WAIT_PAYMENT = 점유 아님  
👉 CONFIRMED = 진짜 예약  

---
## 6️⃣ Reservation Entity
### 상태 전이 규칙 
- confirm(clock)
- expire(clock)  
👉 도메인 책임 분리  

---
## 7️⃣ ReservationService 흐름
### reservce()
1. 시간 검증
2. StudyRoom 락
3. 정책 검증 (RESERVE phase)
4. CONFIRMED 기준 중복 체크
5. WAIT_PAYMENT 저장
---
### confirmPayment()
1. StudyRoom 락
2. PAYMENT 정책검증
3. 중복 재검증
4. reservation.confirm()

---

## 예약 흐름 정리 
```java
reserve()
 └ WAIT_PAYMENT 저장

confirmPayment()
 ├ 상태 WAIT_PAYMENT인지 확인
 ├ 10분 지났는지 확인
 ├ CONFIRMED랑 충돌 확인
 └ CONFIRMED로 변경

scheduler()
 └ WAIT_PAYMENT 중 10분 지난 것 EXPIRED
```
