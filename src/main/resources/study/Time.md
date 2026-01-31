# 시간
시간을 보통 `LocalDateTime.now()`를 사용해서 현재 시간을 표현하려 했음
```java
private void timeValidator(LocalDateTime start, LocalDateTime end) {
    if (!start.isBefore(end) || start.isBefore(LocalDateTime.now())) {
        throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
    }
}
```
논리적으로는 맞음 : 1. start < end가 아니면 실패 2. start < 현재시간이면 실패  

## 실무에서 위험한 포인트
❌LocalDateTime.now() 직접 호출

### 문제 1 - 테스트가 불안정해짐
테스트 실행 시점에 따라:
```text
now = 10:00:00.001
start = 10:00:00.000
```
-> 1ms 차이로 테스트 실패 가능  
즉, 테스트가 운에 따라 깨지는 flaky test가 될 수 있음

---
### 문제 2 - 서버 시간 의존
- 서버 시간 오차
- 타임존 설정 문제
- 운영 서버/ 로컬 차이  

실서비스에서 장애 원인이 된다

---
## 실무에서 권장하는 방식
"현재 시간"을 직접 호출하지 말고 주입하라

### 가장 깔끔한 방식 - Clock 주입
#### 📌Config
```java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone();
}
```
#### 📌Service
```java
private final Clock clock;

private void timeValidator(LocalDateTime start, LocalDateTime end) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (!start.isBefore(end) || start.isBefore(now)) {
        throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
    }
}
```
#### 장점
- 테스트에서 시간 고정 가능
- 재현 가능한 테스트
- 실무 표준 방식

---
### 간단하게 가는 방식 (학습용)
```java
private void timeValidator(LocalDateTime start, LocalDateTime end) {
    LocalDateTime now = LocalDateTime.now();

    if (!start.isBefore(end) || start.isBefore(now)) {
        throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
    }
}
```
테스트에서:
```java
LocalDateTime.now().plusMinutes(10)
```
같이 여유를 주면 안정적