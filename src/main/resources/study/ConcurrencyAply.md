# 동시성 문제 해결 흐름

## 지금은 일단
프로젝트 규모도 작고 학습 목적이기에  
**비관적 락 (PESSIMISTIC_WRITE)로 만들어 보겠음

예약 생성 트랜잭션 안에서 StudyRoom 행을 FOR UPDATE로 잠근 다음,  
그 락을 잡은 상태에서 "겹침 존재 여부"를 검사하고 저장한다  
그러면 같은 룸을 동시에 예약하려는 요청은 **락에서 대기**하게 돼서, 둘 다 통과하는 일이 사라진다

## StudyRoomRepository에 "락 걸고 조회" 메서드 추가
### ✅JPQL + @Lock 방식 (가장 흔함)
```java
package me.studyroom.domain.studyRoom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudyRoom s where s.id = :id")
    Optional<StudyRoom> findByIdForUpdate(@Param("id") Long id);
}
```
- PESSIMISTIC_WRITE -> DB가 보통 SELECT ... FOR UPDATE로 실행해줌
- "특정 스터디룸 row"만 잠그는거라 락 단위가 작음 (매장만 닫기)

---

## ReservationService.reserve()를 트랜잭션으로 만들고, 락 조회로 바꾸기 
중요 : 락은 트랜잭션이 있어야 유지됨  
그래서 @Transactionl이 필수 
```java
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final CommonService commonService;

    @Transactional
    public ReservationResponse.Create reservation(ReservationRequest.Create request, Long userId) {

        User user = commonService.getUserById(userId);

        // ✅ 여기서 락 걸고 룸 조회
        StudyRoom studyRoom = studyRoomRepository.findByIdForUpdate(request.studyRoomId())
            .orElseThrow(() -> new StudyRoomException(ExceptionCode.NOT_FOUND_STUDYROOM));

        studyRoom.ensureAvailable();

        boolean exists = reservationRepository.existsOverlappingReservationExceptSelf(
            studyRoom,
            ReservationStatus.RESERVED,
            request.startAt(),
            request.endAt(),
            -1L // reserve(신규)는 self 제외가 필요 없으면 별도 메서드로 분리하는 게 더 깔끔
        );

        // 신규 예약용으로는 보통 self 제외 없는 exists 메서드를 따로 둬.
        // 여기선 네 코드 흐름에 맞춰 설명만 해둘게.

        if (exists) {
            throw new ReservationException(ExceptionCode.SCHEDULE_CONFLICT);
        }

        Reservation reservation = Reservation.create(user, studyRoom, request.startAt(), request.endAt());
        reservationRepository.save(reservation);

        return new ReservationResponse.Create(
            user.getName(),
            studyRoom.getName(),
            request.startAt(),
            request.endAt()
        );
    }
}
```
## "락이 정말 걸리는지" 이해 포인트
동시에 같은 룸 예약이 들어오면 : 
- T1 : `findByIdForUpdate(roomId)` -> room row 락 획득
- T2 : `findByIdForUpdate(roomId)` -> 여기서 대기 
- T1 : 겹침 심사 -> 저장 -> 트랜잭션 종료(커밋) -> 락 해제 
- T2 : 이제 락 획득 -> 겹침 검사에서 "이미 예약 있음" -> 실패  

즉, 로직의 "조회->저장"이 락으로 인해 **실질적으로 직렬화**됨

---
## updateReservation에도 같은 락을 거는게 좋나? 
같은 룸/시간대에 영향 주는 작업이라 update도 권장됨  
특히 "시간 변경"이면 결국 신규 예약과 똑같이 경쟁이 발생할 수 있음

권장 흐름:
1. reservationRepository.findByIdAndUserId(...)로 내 예약 가져오기
2. 변경 대상 StudyRoom을 findByIdForUpdate로 락 걸고 조회
3. exists ... ExceptSelf로 겹침 검사
4. 업데이트

---
## 질문
### DB락이 정확히 "어디에 걸리는가"?
지금 코드에서 락이 걸리는 대상은  
StudyRoom 테이블 전체가 아니라, 특정 StudyRoom "row(행)" 하나다  

### 코드 
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select s from StudyRoom s where s.id = :id")
Optional<StudyRoom> findByIdForUpdate(Long id);
```
이게 실행되면 JPA는 DB에 이런 SQL을 날림 (대략):
```sql
SELECT *
FROM study_room
WHERE id = ?
FOR UPDATE;
```
### FOR UPDATE가 의미하는 것 
DB(MySQL 기준)에서 : 
```sql
SELECT ... FOR UPDATE
```
👉조회된 row들만 잠근다 (Row Lock)
즉 : 
- ❌study_room 테이블 전체를 잠그는게 아니고
- ❌reservation 테이블의 FK를 잠그는 것도 아니고
- ✅조건에 맞는 study_room 테이블의 특정 행(row)만 잠근다

## 주의할 점 
Row Lock은 :
- 같은 row를 수정/락하려는 트랜잭션만 막는다
- 단순 SELECT는 막지 않는다 (격리 수준에 따라 다름)

그래서 : 
- 락은 반드시 **트랜잭션 안에서 유지**
- 락 잡은 뒤 빠르게 로직 수행 후 커밋
- 락 오래 잡으면 성능 저하 발생

