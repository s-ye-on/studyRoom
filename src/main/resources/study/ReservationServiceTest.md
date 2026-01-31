# ReservationServiceTest에서 Clock 의존성 

## 문제가 있었던 기존 방식 
```java
LocalDateTime.now()
```
이 방식은 테스트에서 치명적인 불안정성 만든다 

### 문제점 
- 테스트 실행 시점에 따라 결과가 달라짐
- 자정, 시간 경계, 실행 지연 시 테스트가 랜덤하게 깨짐
- "과거 시간 예약 불가" 같은 로직을 **정확히 검증 불가**

테스트가 비즈니스 규칙을 검증하는게 아니라 **시계 상태에 의존한다**

---
## ✅해결 : 시간도 의존성으로 취급 
```java
public class ReservationService {
	private final Clock clock;
}
```
```java
LocalDateTime.now(clock);
```
이 설계의 의미 
- 시간은 **외부 환경**
- 외부 환경은 **제어 가능해야 테스트 가능**
- 시간은 값이 아니라 **의존성**

---
## 테스트에서 왜 @MockitoBean Clock을 썼나?
```java
@MockitoBean
private Clock clock;
```
### 이유 
- 운영 코드에는 이미 TimeConfig가 있다
- 테스트에서만 **기존 Bean을 안전하게 교체**해야 함 
- @TestConfiguration + @Import는  
    -> Bean 중복/override 충돌 위험 있음

### Mockito 방식의 장점 
- 기존 Clock Bean **완전히 교체**
- ApplicationContext 충돌 없음 
- 테스트마다 시간 자유롭게 설정 가능 
```java
Mockito.when(clock.instant()).thenReturn(fixedInstant);
Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());
```

### 요약 (Clock)
| 항목                       | 이유          |
|--------------------------|-------------|
| Clock 사용                 | 시간은 외부 의존성  |
| mock 사용                  | 테스트에서 시간 고정 |
| LocalDateTime.now(clock) | 테스트 결정성 확보  |
| MockitoBean              | Bean 충돌 방지  |

## 정리 
```text
"예약 서비스에서 시간은 외부 환경이기 때문에
Clock을 의존성으로 분리했고 
테스트에서는 mock을 사용해 시간을 고정했다

또한 예약 충돌 검증은 반열린 구간 [start, end)모델을 따르며
경계 시간은 겹치지 않는 것이 도메인 규칙
```
