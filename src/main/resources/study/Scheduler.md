# 스케줄러
사용처 : 예약을 하고 결제가 되지않고 10분이 지나면 만료로 바꾸고 싶음  
결제 단을 호출하지 않으면 영원히 `WAIT_PAYMENT` 상태로 있기에 관리에 어려움이 있을것이라 생각했음

## 고려 사항
1. Reservation에 createdAt이 있어야함 
2. Repository에 이런 쿼리가 필요함 :  
   - `WAIT_PAYMENT`이고 createdAt <=now - 10분 대상 찾기/업데이트
3. 스케줄러에서 Clock을 써서 now를 만들고 
4. 벌크 업데이트면 @Modifying + clearAutomatically 고려

### @Modifying은 언제쓰고 무슨 역할? 
Spring Data JPA에서 @Query는 기본이 **SELECT 전용**  
그래서 UPDATE / DELETE 쿼리를 @Query로 날리려면 반드시 @Modifying을 붙여줘야한다  

언제 사용?  
- Update reservation SET status = ... WHERE ...  
- DELETE FROM reservation WHERE ... 
- 이런 **데이터 변경 쿼리**를 리포지토리에서 직접 실행할 때 사용  

무슨 기능?  
- "이 쿼리는 조회가 아니라 변경 쿼리야"라고 Spring Date JPA에 알려주고,  
- 실행 결과를 **영향 받은 row 수(int)** 같은 걸로 돌려줄 수 있게 해줌

### clearAutomatically는 언제쓰고, 무슨 역할? 
벌크 업데이트(UPDATE/DELETE)는 **영속성 컨텍스트(1차 캐시)** 를 무시하고 DB에 바로 때려박는다  
그래서 같은 트랜잭션 안에서 이미 Reservation 엔티티를 조회해 1차 캐시에 들고 있으면 :  
- DB에서는 status가 EXPIRED로 바뀌었는데
- 영속성 컨텍스트 안의 객체는 아직 `WAIT_PAYMENT`인 **옛 상태**일 수 있다 (벌크 업데이트의 대표적인 함정)  

`clearAutomatically = true` 는 이 문제를 줄이려고,  
**쿼리 실행 후 자동으로 EntityManager.clear()를 호출해서 1차 캐시를 비워버리는 옵션**  

언제 켜는게 좋나? 
- 벌크 업데이트 직후 같은 트랜잭션에서 **같은 엔티티를 다시 읽거나 로직을 이어갈 때**  
- 상태가 뒤틀릴 가능성이 있는 서비스 로직에서 안전하게 가고 싶을 때  

언제 굳이 안켜도 되나? 
- 스케줄러처럼 "벌크 업데이트 한 번 하고 끝"이고
- 그 트랜잭션 안에서 해당 엔티티를 더 안 만질 때 -> 사실 큰 차이 없고 켜도 무방  

### flushAutomatically? 
- `flushAutomatically=true` : 벌크 쿼리 실행 전에 영속성 컨텍스트 변경 사항을 먼저 DB에 flush
- 보통 복잡한 트랜잭션에서 안전장치로 씀 

---

## 구현
### 1. Repository: 벌크 업데이트 메서드 
ReservationRepository
```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Reservation r
           set r.status = :expired
         where r.status = :wait
           and r.createdAt < :deadline
    """)
    int expireWaitPayments(
            @Param("wait") ReservationStatus wait,
            @Param("expired") ReservationStatus expired,
            @Param("deadline") LocalDateTime deadline
    );
```
- `clearAutomatically = true` : 벌크 업데이트 후 1차캐시 비워서 상태 꼬임 방지 
- `flushAutomatically = true` : 혹시 같은 트랝개션에서 쌓인 변경 사항이 있으면 먼저 flush 
---
### 2. Scheduler Service : 주기적으로 만료 처리 
"현재 시간- 10분" 기준으로 EXPIRE 처리 
```java
package me.studyroom.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import me.studyroom.domain.reservation.ReservationRepository;
import me.studyroom.domain.reservation.ReservationStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 10;

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    // 1분마다 실행 (원하는 주기로 바꿔)
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireWaitPayments() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        int updated = reservationRepository.expireWaitPayments(
                ReservationStatus.WAIT_PAYMENT,
                ReservationStatus.EXPIRED,
                deadline
        );

        // 필요하면 로그만
        // log.info("Expired WAIT_PAYMENT reservations: {}", updated);
    }
}
```
fixedDelay는 "이전 실행이 끝난 뒤" 기준이라, 처리시간 길어질 때도 안정적이다  
크론 쓰고 싶으면 `@Scheduled(cron = "0 */1 * * * *")` 이런식으로도 가능

---
### 3. 스케줄러 활성화 설정 
스프링 부트 메인(또는 설정 클래스)에 이게 없으면 스케줄러가 안돈다 : 
```java
@EnableScheduling
@SpringBootApplication
public class StudyRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyRoomApplication.class, args);
    }
}
```