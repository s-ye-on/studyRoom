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