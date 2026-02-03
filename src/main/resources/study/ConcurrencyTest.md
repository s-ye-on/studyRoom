# 동시성 (Concurrency) test

## 이 테스트가 하고 싶은 일 
똑같은 예약 요청을 두 개의 "동시에 실행되는 작업"으로 만들어서  
그 결과 DB에 예약이 1건만 저장되는지 확인한다 

## 1. Thread(스레드)란?
스레드란 쉽게 말해 : 프로그램 안에서 동시에 실행될 수 있는 "일꾼" 한 명  

- 평소 코드는 한 줄씩 순서대로 실행되지만(싱글 스레드)
- 스레드를 여러 개 쓰면 "동시에" 작업이 진행될 수 있다  

테스트에서 : 
- 스레드 2개가 동시에 `reservationService.reservation()` 를 호출하게 만들었다

## 2. ExecutorService란?
```java
ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
```
이건 스레드를 직접 만들고 관리하기 귀찮으니까, **스레드 풀(일꾼 풀)**을 만들어서 일을 맡기는 도구 
- `newFixedThreadPool(2)` -> 일꾼 2명 고정으로 고용
- 우리가 해야할 일(task)을 던지면
- 풀 안의 일꾼(스레드)이 그 일을 가져가서 실행해줌

---

## 3. Runnable이란? 
```java
Runnable task = () -> { ... };
```
Runnable은:
**"실행할 코드 묶음(작업)"을 표현하는 인터페이스**  

정확히는 메서드가 하나뿐이다 :  
```java
void run();
```
즉 Runnable은 "이 코드(run)를 실행해라"라는 작업 단위  

그래서: 
```java
executorService.submit(task);
```
이렇게 하면 스레드 풀이 task.run()을 실행

---
## 4. CountDownLatch
동시성 테스트에서 핵심은 :  
진짜로 동시에 시작하게 만들기  

그냥 스레드 2개 만들었다고 "완벽히 동시에" 시작하지 않는다  
OS 스케줄링 때문에 시작 타이밍이 조금씩 어긋날 수 있다  

그래서 우리가 "출발선 맞추기"를 하는게 CountDownLatch  

### 4-1 readyLatch : "둘 다 준비됐어?" 확인
```java
CountDownLatch readyLatch = new CountDownLatch(threadCount);
```
- 카운트가 2부터 시작
- 각 스레드가 준비되면 countDown()해서 1씩 줄임
- 메인 스레드는 readyLatch.await()로 기다림
- 카운트가 0이 되면 "둘 다 준비 끝" 신호

스레드 안에서: 
```java
readyLatch.countDown();
```
메인에서:
```java
readyLatch.await();
```
### 4-2 startLatch "출발" 버튼
```java
CountDownLatch startLatch = new CountDownLatch(1);
```
이건 출발 신호기  
스레드들은 출발 신호가 올 때까지 대기 :  
```java
startLatch.await();
```
메인 스레드가 "출발" 한 번 누르면 : 
```java
startLatch.countDown();
```
그 순간 대기 중이던 스레드들이 **동시에 풀려서** 달려 나간다

### 4-3 doneLatch : "다 끝났어?"확인
```java
CountDownLatch doneLatch = new CountDownLatch(threadCount);
```
각 스레드가 작업을 끝낼 때마다 : 
```java
doneLatch.countDown();
```
메인 스레드는:
```java
doneLatch.await();
```
로 기다렸다가  

"둘 다 끝났음"이 확인된 후에는 DB를 조회해서 검증하는 것 

---
## 5. AtomicInteger는 왜 썼을까 
```java
AtomicInteger successCount = new AtomicInteger();
AtomicInteger failCount = new AtomicInteger();
```
여기서도 "동시성"이 등장  
일반 int는 스레드가 동시에 증가시키면 깨질 수가 있다  
예를 들어 스레드 2개가 동시에 success++하면:  
- 둘 다 같은 값을 읽고
- 둘다 +1해서
- 한 번 증가만 반영되는 문제 같은게 생김 (경쟁 조건)  

AtomicInteger는:  
여러 스레드가 동시에 값을 바꿔도 안전하게 증가 시켜주는 숫자 객체  

그래서:
```java
successCount.incrementAndGet();
failCount.incrementAndGet();
```
이건 멀티스레드에서도 정확히 카운트가 맞다

---
## 6. task 내부 흐름
```java
Runnable task = () -> {
    try {
        readyLatch.countDown();
        startLatch.await();

        reservationService.reservation(createRequest, userId);
        successCount.incrementAndGet();

    } catch (Exception e) {
        failCount.incrementAndGet();
    } finally {
        doneLatch.countDown();
    }
};
```
### 6-1 준비 완료 표시
- "나 준비했어"하고 readyLatch 내림

### 6-2 출발 신호 기다림
- startLatch.await()에서 멈춰서 대기 
- 메인 스레드가 출발 신호 주면 동시에 진행

### 6-3 예약 시도
- 한 명은 락을 잡고 성공
- 다른 한 명은 락이 풀린 뒤 검샇면 겹침 발견 -> 예외 -> 실패 

### 6-4 성공/실패 카운트 기록
- 성공하면 successCount++
- 실패하면 failCount++

### 6-5 doneLatch 내림
- 어떤 결과든 "내 작업 끝"을 알려줘야 메인이 계속 진행 가능

---
## 7. 메인 스레드(테스트 메서드)흐름
```java
for (...) executorService.submit(task);

readyLatch.await();
startLatch.countDown();
doneLatch.await();

List<Reservation> reservations = reservationRepository.findAll();
assertThat(reservations.size()).isEqualTo(1);
```
- 작업 2개를 스레드풀에 던짐
- 둘 다 출발선에 섰는지 확인(readyLatch.await)
- 출발 신호 (startLatch.countDown)
- 둘 다 끝날 때까지 기다림(doneLatch.await)
- DB에 예약이 1개인지 확인

## 8. 왜 락이 효과있으면 1개만 저장될까? 
내가 한 락: 
- Thread-1이 StudyRoom row를 잠금
- Thread-2는 그 row를 잠글 때까지 대기  

Thread-1이 예약을 먼저 저장하면,  
Thread-2는 락이 풀린 후 조회했을 때 이미 예약이 생겼으니까  
중복 검사에서 결려서 예외가 터짐

## @Transactional에 관하여 
```text
Spring의 트랜잭션은 ThreadLocal 기반이기 때문에 
트랜잭션은 스레드를 넘을 수 없다 
따라서 멀티스레드 테스트에서
테스트 메서드에 @Transactional을 붙이면
다른 스레드의 커밋 결과를 관찰할 수 없게 된다 
동시성 테스트에서는 테스트 트랜잭션을 제거하고 
서비스 트랜잭션만을 검증해야 한다 
```

### ThreadLocal 이란? 
트랜잭션은 '스레드에 붙어 있다'  

Spring의 `@Transactional`은 내부적으로 ThreadLocal을 사용하여  
"현재 스레드가 사용중인 트랜잭션"을 저장  

#### ThreadLocal
- 스레드 A -> 자기 전용 공간
- 스레드 B -> 자기 전용 공간
- 서로 절대 공유 안됨  
👉스레드가 다르면 트랜잭션도 다르다 

#### 결론
"하나의 트랜잭션 안에서 여러 스레드가 실행된다" ❌  
불가능하다  
트랜잭션은 절대 스레드를 넘지 못한다  

동시성 테스트에서 @Transactional을 붙이지 않으면  
테스트 메서드 수준의 트랜잭션이 생기지 않고  
스레드의 트랜잭션만 있어서 그걸 관찰하는 것 