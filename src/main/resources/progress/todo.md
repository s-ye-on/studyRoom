# todo 

## 26.02.07
### WAIT_PAYMENT 만료 처리 
confirmPayment 안 부르면 영원히 WAIT_PAYMENT 상태

#### 필요한 것 
##### 1) 만료 처리 엔드 포인트
```java
POST /reservations/{id}/expire-check
```

또는 
```java
@Service
public void expireIfNeeded(Long reservationId)
```
---
##### 2) 배치/스케줄러
```java
@Scheduled(fixedRate = 1분)
void expireReservations()
```
- WAIT_PAYMENT 중
- createdAt + 10분 초과  
👉EXPIRED로 전환 

---

### PaymentPolicy 실제 구현
지금은 껍데기임 결제 모듈은 나중에 붙여도 인터페이스는 지금 완성 가능

---
### update 규칙 보강
- CONFIRMED만 수정 가능
- WAIT_PAYMENT는 수정 불가 
- EXPIRED는 당연 불가  
👉엔티티 + 서비스 같이 보강

---
### 테스트 대공사 ㅋㅋ
- 현재 테스트는 RESERVED 기준
- reserve() -> WAIT_PAYMENT
- confirmPayment() -> CONFIRMED
- 이후 중복 체크 

---
### 정책 문서 정리 