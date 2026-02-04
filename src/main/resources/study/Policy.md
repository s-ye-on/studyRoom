# 정책 
- 운영 시간 이외 예약 금지

## 구현 

### 정책은 "판단 로직"만, 조회는 외부에서 
```java
class DailyLimitPolicy implements ReservationPolicy {

    @Autowired
    ReservationRepository repo;   // ← 정책 안에서 직접 조회?

    void validate(request, studyRoom, user) {
        int count = repo.countToday(user);  // 여기서 조회
        if (count >= 3) throw ...
    }
}
```
만약 이렇게 정책에서 직접 DB 조회할 경우 생기는 문제 : 
- 정책이 인프라에 의존 
- 테스트가 빡쎄짐 
- 재사용 불가 
  - 정책이 특정 구현(DB)에 묶임 -> 다른 환경에서 못 씀  

더 좋은 구조  
정책 = 순수한 판단 로직  
조회 = 서비스 책임 

## 여러 정책을 전부 통과해야하는 경우 (AND 구조 )
### 의도 
- 운영 시간도 맞아야 하고 
- 최대 예약 시간도 맞아야 하고
- 회원 정책도 맞아야 함  
👉전부 OK여야 예약 성공 

### 코드 
Service 
```java
private final List<ReservationPolicy> policies;

public void reserve(...) {

    for (ReservationPolicy policy : policies) {
        policy.validate(start, end, studyRoom);
    }

    // 이후 예약 로직
}
```
구현체들  
```java
@Component
public class OperatingTimePolicy implements ReservationPolicy
```
```java
@Component
public class MaxDurationPolicy implements ReservationPolicy
```
```java
@Component
public class MembershipPolicy implements ReservationPolicy
```
### 특징 
- 확장 쉬움
- 정책 추가 = 클래스 하나 
- OCP 완벽
- 실제 현업에서 제일 많이 사용  

## 상황에 따라 하나만 선택하는 경우 
### 예시 
- 일반 예약 → OperatingTimePolicy
- VIP 예약 → VipPolicy
- 관리자 예약 → AdminPolicy

### 코드 
#### 1. Qualifier 방식 
```java
private final ReservationPolicy reservationPolicy;
```
```java
@Bean
@Qualifier("vip")
public ReservationPolicy vipPolicy() { ... }
```
#### 전략 선택 로직 
```java
ReservationPolicy policy =
    policyFactory.getPolicy(userType);

policy.validate(...);
```
### 특징
- 분기 로직 필요
- 조건별 전략 선택
- "하나만" 적용  

---
## 정책 객체 개념
### 정책이란?
예약 가능 여부를 판단하는 비즈니스 규칙

#### 예전 스타일
```java
if (start > end) ...
if (!room.available) ...
if (overlap) ...
if (membership) ...
```
👉서비스가 규칙 공장이 된다❌

---
### 정책 객체 목적 
- 규칙을 서비스에서 분리
- 변경 포인트 격리
- 테스트 쉬움
- 재사용 가능

---
### 역할 분리 
#### 엔티티
- 자기 상태 무결성
- "나는 이런 존재다"
```java
studyRoom.ensureAvailable();
```

#### 정책
- 비즈니스 규칙 
- "이 상황이 허용되는가"
```java
policy.validate(start, end, room);
```
#### 서비스
- 흐름 제어 
- 트랜잭션
- 협력 조율 

---
### 좋은 정책 구조 
```java
public interface ReservationPolicy {
    void validate(LocalDateTime start,
                  LocalDateTime end,
                  StudyRoom room);
}
```
구현체는 관심사 하나만: 
```java
@Component
public class OperatingTimePolicy implements ReservationPolicy {
    @Override
    public void validate(...) {
        // 운영시간만 봄
    }
}
```
--- 
인터페이스 매개변수는 구현에서 다 안써도 된다 

---
## 핵심 3줄 
1. 정책 객체 = "검증 규칙을 서비스에서 분리한 전략 객체"
2. 여러 정책 → List 주입 / 하나만 → Qualifier
3. 인터페이스 매개 변수는 "필요한 최대 컨텍스트", 구현체는 일부만 써도 OK