# 동시성 문제 Concurrency Problem

## 예시 상황
스터디룸 A  
시간 : 14:00~ 16:00  

동시에 두 사람이 예약 버튼 클릭: 
```java
T1 (사용자1) → 중복 검사 → 아직 예약 없음
T2 (사용자2) → 중복 검사 → 아직 예약 없음
T1 → 예약 저장
T2 → 예약 저장
```
👉 결과 : **같은 시간 예약 2개 생성됨 (버그)** ❌  
이게 바로 **Race Condition(경쟁 상태)**
---
## 왜 이런일 발생하나? 
현재 로직은:
```java
1. DB에서 겹치는 예약 있는지 조회
2. 없으면 저장
```
이 두 단계가 **원자적(atomic)**이지 않기 때문  

두 요청이 동시에 들어오면:
- 둘 다 "없다"고 판단해버림
- 둘 다 저장 성공

---
## 🔐 "락"으로 이걸 막을 수 있다
### 백화점 잠금 비유
- 백화점 전체 문닫기 -> 전체 lock (성능 최악, 안전 최고)
- 특정 매장만 닫기 -> 부분 lock (성능 좋고 실무적)  

예약 시스템에서도 마찬가지

---
## 락을 걸 수 있는 레벨들 

### 🟥Level 1 - 애플리케이션 락 (synchronized)
```java
synchronized reserve() { ... }
```
❌서버 여러 대면 의미 없음  
❌실무에서 거의 안씀

---

### 🟧Level 2 - DB 테이블 락
```sql
LOCK TABLE reservation;
```
❌모든 예약 막힘  
❌성능 재앙  
❌실무 사용 거의 없음

---

### 🟨Level 3 - DB Row 락 (실무 핵심)
👉특정 row만 잠근다  

예:
- 특정 StudyRoom row만 락
- 특정 예약 row만 락  

이 부분이 특정 매장만 닫는 것

---

## 실무에서 예약 시스템이 쓰는 대표 전략 2가지 
### ✅전략 A - 비관적 락 (Pessimistic Lock)
"어차피 충돌 날 것 같으니 미리 잠가버리자"

#### 예시 
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
StudyRoom findByIdForUpdate(Long id);
```
##### Service : 
```java
StudyRoom room = studyRoomRepository.findByIdForUpdate(id);
room.ensureAvailable();

// 이제 이 room row는 다른 트랜잭션이 못 건드림
중복 검사
저장
```
#### 장점
- 충돌 확실히 방지
- 구현 단순
- 논리 명확

#### 단점
- 동시 요청 많으면 대기 증가
- 트래픽 많으면 성능 저하 

---

### 전략 B - 낙관적 락 (Optimistic Lock)
"일단 믿고 가고, 충돌 나면 롤백하자"

#### 엔티티에 version 추가
```java
@Version
private Long version;
```
JPA가 자동으로 버전 충돌 감지함

#### 장점
- 락 대기 없음
- 성능 좋음

#### 단점
- 충돌 시 재시도 로직 필요
- 설계 복잡

---

## 지금은 일단
프로젝트 규모도 작고 학습 목적이기에  
**비관적 락 (PESSIMISTIC_WRITE)로 만들어 보겠음

예약 생성 트랜잭션 안에서 StudyRoom 행을 FOR UPDATE로 잠근 다음,  
그 락을 잡은 상태에서 "겹침 존재 여부"를 검사하고 저장한다  
그러면 같은 룸을 동시에 예약하려는 요청은 **락에서 대기**하게 돼서, 둘 다 통과하는 일이 사라진다


### 기존 흐름 
```java
1. StudyRoom 조회
2. 중복 예약 검사
3. 저장
```

### 락 적용 흐름
```java
1. StudyRoom 조회 + Row Lock
2. ensureAvailable()
3. 중복 예약 검사
4. 저장
5. 트랜잭션 종료 → 락 해제
```
---

## 핵심 요약
| 개념               | 의미                |
|------------------|-------------------|
| Race Condition   | 동시에 접근해서 데이터 깨짐   |
| Lock             | 동시에 못 들어오게 막음     |
| Row Lock         | 특정 데이터만 잠금(실무 핵심) |
| Pessimistic Lock | 미리 잠그는 전략         |
| Optimistic Lock  | 충돌 시 롤백 전략        |

