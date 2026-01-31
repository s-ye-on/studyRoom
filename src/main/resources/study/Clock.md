## Clock

지금은 LocalDateTime.now()를 사용중 

## Clock은 무엇? 
**Clock은 "지금 시간(now)을 직접 만들지 말고, 외부에서 주입받자"는 개념**

즉,
```java
LocalDateTime.now()
```
이걸 쓰지말고 
```java
LocalDateTime.now(clock)
```
✅이렇게 쓰자

---
## 왜 LocalDateTime.now()가 위험할까
지금 서비스 코드 
```java
if (start.isBefore(LocalDateTime.now())) {
    throw new ReservationException(...)
}
```
이 코드의 문제점 :  

| 문제         | 설명              |
|------------|-----------------|
| 테스트 불안정    | 실행 타이밍마다 결과 달라짐 |
| 동시성 테스트 실패 | 스레드 간 시간 차 발생   |
| 재현 불가 버그   | 운영에서만 터질 수 있음   |
| 시간 조작 불가   | 과거/미래 테스트 불가능   |

👉"코드가 시간을 통제한다"는게 문제

---
## Clock이 있으면 어떤 것이 달라질까
Clock 사용 시 이런게 가능해진다 
```java
Clock fixedClock = Clock.fixed(
    Instant.parse("2026-01-01T00:00:00Z"),
    ZoneId.systemDefault()
);
```
그럼 이제 : 
```java
LocalDateTime.now(clock)
```
은 언제 호출해도 항상 같은 시간  

### 결과적으로 
| 항목    | before | after |
|-------|--------|-------|
| 테스트   | 불안정    | 항상 동일 |
| 동시성   | 터짐     | 안정    |
| 시간 검증 | 랜덤     | 예측 가능 |
| 설계    | 암묵적    | 명시적   |

---
## Clock은 "시간에 대한 의존성 주입"이다 
Clock = Repository/Service랑 **동급의 의존성**

현재 서비스 생성자는 이렇다 
```java
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final CommonService commonService;
}
```
👉여기에 Clock이 추가되는 것

```java
private final Clock clock;
```

---
## Clock은 어디에 두는게 맞나? 
정답 : 설정 클래스 (@Configuration)
```java
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
```
이유 : 
- 애플리케이션 전체에서 **하나의 시간 기준**
- 테스트에서는 이 Bean을 **override 가능**

---
## 서비스에서 Clock을 쓰는 방식 
### 🔴기존(위험)
```java
start.isBefore(LocalDateTime.now())
```
### 🟢변경 후 
```java
start.isBefore(LocalDateTime.now(clock))
```
이제 이 서비스는 : 
- "지금이 언제인지" **알지 못함**
- 단지 "Clock에게 물어봄"  

이게 **테스트 가능한 설계**

---
## 이게 왜 실무에서 중요하나
Clock을 쓰면 이런게 가능해진다 
### ✔ 야간 배치 테스트 
```text
"자정 이후에는 예약 불가"
```

### ✔ 이벤트 기간 테스트
```text
"2026-01-01 ~ 2026-01-07 할인"
```

### ✔ 타임존 테스트
```text
KST / UTC / 해외 사용자
```

### ✔ 동시성 테스트
```text
모든 스레드가 동일한 현재 시간 사용
```
---

시간도 의존성이다  
테스트가 설계를 이끈다  

---
# 코드 

## Clock 설정 클래스 
```java
package me.studyroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

	@Bean
	public Clock clock() {
		// 운영 환경 기본 시간
		return Clock.systemDefaultZone();
	}
}
```
왜 이곳인가? 
- @Configuration -> 전역 설정
- 시간은 **도메인/서비스 공통 관심사**
- 테스트에서 **override 하기 쉬움**

---
## ReservationService에 Clock 주입 
### 🔴기존 생성자 
```java
@RequiredArgsConstructor
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final StudyRoomRepository studyRoomRepository;
	private final CommonService commonService;
}
```
### 🟢변경 후
```java
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final StudyRoomRepository studyRoomRepository;
	private final CommonService commonService;
	private final Clock clock;
}
```
"시간도 외부 의존성이다" <- 핵심 설계 포인트  

---
## LocalDateTime.now() 전부 교체 
### 🔴기존 (위험)
```java
start.isBefore(LocalDateTime.now())
```
### 🟢변경 후 (정석)
```java
start.isBefore(LocalDateTime.now(clock))
```
### timeValidator 최종 형태
```java
private void timeValidator(LocalDateTime start, LocalDateTime end) {
	LocalDateTime now = LocalDateTime.now(clock);

	if (!start.isBefore(end) || start.isBefore(now)) {
		throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
	}
}
```
✔ 이제 시간 기준이 **고정 가능**
✔ 동시성 테스트에서 스레드 간 시간 차 제거 됨

---
## ServiceTEst에서 Clock 고정하기 
### ReservationTest에 테스트 전용 Clock 주입 
```java
@TestConfiguration
static class TestClockConfig {

	@Bean
	public Clock clock() {
		return Clock.fixed(
			Instant.parse("2026-01-01T00:00:00Z"),
			ZoneId.systemDefault()
		);
	}
}
```
그리고 테스트 클래스에 추가 
```java
@SpringBootTest
@Import(TestClockConfig.class)
class ReservationServiceTest {
```
이제 모든 테스트에서 : 
```java
LocalDateTime.now(clock)
```
👉 항상 2026-01-01 09:00 (KST) 기준

---
## 동시성 테스트가 왜 이제 통과되나? 
### 🔴이전 상황 
- Thread A → now = 19:07:25.933
- Thread B → now = 19:07:25.934
- startAt  비교 중 **과거 판정**
- 둘 다 INVALID_TIME_RANGE 터짐

### 🟢Clock 도입 후 
- 모든 스레드가 **같은 fixed time**
- 시간 검증 통과
- 락 + 중복 검사 로직만 작동
- 👉성공 1/ 실패 1