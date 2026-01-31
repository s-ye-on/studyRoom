# 성능 인덱스

## 인덱스란?
인덱스는 DB에서 "검색을 빠르게 하기 위한 정렬된 목차"  

책으로 비유하자면:
- 인덱스 없음 -> 1페이지부터 끝까지 다 넘겨서 찾기 (Full Scan)
- 인덱스 있음 -> 목차 보고 바로 해당 페이지 이동

---
## 내 코드에서 핵심 쿼리
```sql
WHERE r.studyRoom = :studyRoom
  AND r.startAt < :endAt
  AND r.endAt > :startAt
  AND r.status = 'RESERVED'
```
이 쿼리는 의미상 :  
특정 스터디룸의 예약들 중에서  
특정 시간 구간과 겹치는 예약이 있는지 찾는다

### ❌인덱스가 없다면?
DB는 : 
```java
reservation 테이블 전체 스캔
→ 모든 row 하나씩 조건 비교
→ 겹치는 게 있는지 판단
```
예약이 10만건, 100만건 되었을 시 성능 저하

### ✅인덱스가 있다면? 
DB는 : 
```java
study_room_id 기준으로 먼저 좁힘
→ status로 한 번 더 필터
→ startAt / endAt 범위 검색
```
처음부터 후보군을 극단적으로 줄여버림  
이게 성능 차이를 만듬

---

## 어떤 컬럼에 인덱스를 걸어야할까? 
핵심 원칙:
WHERE 조건에 자주 등장하는 컬럼  

내 쿼리 기준:
1. study_room_id (가장 중요)
2. status
3. start_at
4. end_at

추천 인덱스 :
```text
(study_room_id, status, start_at, end_at)
```
이걸 **복합 인덱스(Composite Index**)라고 부름

---
## 왜 이 순서인가? 
인덱스는 왼쪽부터 차례대로 효율이 적용됨
```java
(study_room_id, status, start_at, end_at)
 ↑              ↑
 가장 중요       다음 중요
```
DB는 : 
1. 특정 room만 먼저 추림
2. RESERVED만 남ㄴ김
3. 시간 범위 비교  

이 순서로 탐색을 최적화함

---

## JPA에서 인덱스 거는 방법
Reservation 엔티티에 이렇게 추가하면 된다
```java
@Entity
@Table(
    name = "reservation",
    indexes = {
        @Index(
            name = "idx_reservation_room_status_time",
            columnList = "study_room_id, status, start_at, end_at"
        )
    }
)
public class Reservation {
    ...
}
```
⚠️columnList는 DB 컬럼명 기준이다 (snake_case 주의)

---
## 인덱스도 비용이 있다
인덱스는 만능이 아니다

### ❗️단점
- ❌INSERT / UPDATE 느려짐 (인덱스도 같이 갱신)
- ❌디스크 공간 사용
- ❌인덱스 많으면 오히려 성능 저하 

그래서 :   
👉 "조회가 많고, 조건에 자주 쓰는 컬럼만" 인덱스

---
## 락 + 인덱스 조합 효과
지금 구조:
```java
Room row 락
   ↓
겹침 검사 쿼리 (인덱스 적용)
   ↓
빠르게 결과 반환
   ↓
락 빨리 해제
```
👉락을 오래 잡지 않게 돼서 동시성 성능까지 같이 좋아진다

---
## ⚠️중요한 포인트(실수 많이 하는 부분)
### ✅1. columnList는 DB 컬럼명이다
JPQL 필드명 ❌  
DB 컬럼명 ⭕️  

그래서 : 

| 엔티티 필드    | DB 컬럼         |
|-----------|---------------|
| studyRoom | study_room_id |
| startAt   | start_at      |
| endAt     | end_at        |

이렇게 snake_case로 맞춰야 한다

---
### ✅2. 이미 테이블이 생성된 상태라면?
#### H2 메모리 DB면 :
- 앱 재시작 -> 테이블 재생성 -> 인덱스 자동 생성됨

#### MySQL이면 : 
- 이미 생성된 테이블에는 자동 반영 안됨 
- 직접 ALTER TABLE 필요

---
## 인덱스가 실제로 생성됐는지 확인하기 
### H2 콘솔 접속
```java
http://localhost:8080/h2-console
```
### SQL 실행
```sql
SHOW INDEX FROM reservation;
```
또는 H2에서는 : 
```sql
SELECT * FROM INFORMATION_SCHEMA.INDEXES
WHERE TABLE_NAME = 'RESERVATION';
```
정상이라면:
```java
IDX_RESERVATION_ROOM_STATUS_TIME
```
같은 인덱스가 보임

## ✅ 인덱스 설계는 쿼리를 먼저 보고 -> 인덱스를 설계한다 순