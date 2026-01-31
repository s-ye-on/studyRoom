# 용어 정리 
## Jackson Bean? 
ObjectMapper를 스프링이 관리하는 객체(Bean)로 등록한 것 

## ObjectMapper?
ObjectMapper는: 
```text
자바 객체  ⇄  JSON 문자열
```
을 변환해주는 Jackson 라이브러리의 핵심 클래스

예시 : 
```java
ReservationRequest request = ...
String json = objectMapper.writeValueAsString(request);
```
-> 자바 객체를 JSON 문자열로 바꿈   

또는 : 
```java
ReservationRequest obj =
    objectMapper.readValue(jsonString, ReservationRequest.class);
```
-> JSON을 자바 객체로 바꿈 

---
## Bean? 
Spring에서 Bean은 :  
Spring이 생성하고 관리하는 객체  

```java
@Bean
ObjectMapper objectMapper() { ... }
```
이렇게 등록하면 : 
- Spring이 ObjectMapper를 만들어줌
- 필요한 곳에서 `@Autowired`로 주입 가능  

즉, Jackson Bean = Spring이 관리하는 ObjectMapper

## any() / anyLong() 정확한 의미
```java
any()       // 어떤 객체든 OK (null 제외 기본)
anyLong()   // Long 타입이면 무엇이든 OK (primitive long 포함)
```
✔ 의미  
"이 메서드가 호출될 떄, 이 파라미터 값이 뭐든지 상관하지 않겠다"  
즉, **값 비교가 아니라 타입만 찾으면 매칭 성공**  

⚠️중요한 함정 - null은 기본적으로 매칭 안 됨  
Mockito의 any() 계열은 기본적으로 **null을 매칭하지 않는다**
```java
reserve(any(), anyLong())  // 두 번째 인자가 null이면 매칭 실패
```
그래서 자꾸 테스트에서 
```java
@AuthenticationPrincipal User user
```
이 제대로 주입되지 않으면 user.getId()가 null ->  
service 호출 시 두번째 인자가 null ->  
anyLong()과 매칭 실패 ->  
stub이 적용 안 됨 -> 실제 메서드처럼 동작해버림 -> 201 나옴  

그래서 해결책으로 :
```java
ArgumentMatchers.nullable(Long.class)
```
를 써서 정상 동작하게 만들었다 

---
## Mockito가 말하는 stub이란? 
"Stub"이라는 단어는 원래 **하향식(top-down) 테스트**에서 많이 쓰는 개념  

하지만 Mockito에서 말하는 stub은 조금 더 일반적인 의미 

### 🎯Mockito에서 Stub의 의미 
```java
given(reservationService.reserve(...))
    .willReturn(response);
```
이 한줄이 바로 stub 설정  

의미 : 
"이 메서드가 이렇게 호출되면, 실제 로직을 실행하지 말고 이 값을 반환해라"  

즉: 
- 실제 Service 로직 ❌
- DB 접근 ❌
- 트랜잭션 ❌
- 오직 가짜 응답만 반환 ⭕️

### 그래서 Controller 테스트는 하향식이 맞나? stub을 사용했으니까? 
개념적으로 보면 맞다 
```java
[Controller]  ← 테스트 대상 (상위 계층)
      ↓
[Service]     ← Mock / Stub
      ↓
[Repository]  ← 아예 안 뜸
```
이 구조는 전형적인 **Top-Down 테스트 구조**  
Controller가 "위"  
Service가 "아래"  

Controller의 동작만 검증하고,  
아래 계층은 신뢰하지 않고 Mock으로 대체하는 방식

### 실무 용어로는 이렇게 부름 
- ✔ Slice Test
- ✔ Web Layer test
- ✔ Controller Unit Test  
"하향식 테스트"라는 말은 요즘엔 잘 안쓰고,  
"계층 분리 테스트" 또는 "슬라이스 테스트"라고 많이 부른다 

---
## Mockito가 stub을 못찾는다는 말의 의미 
Mockito가 내부적으로 이렇게 판단:  
"어? 내가 설정한 stub과 실제 호출된 파라미터가 일치하지 않는다"  

그러면 :  
- stub 무시
- 실제 mock 객체의 기본 행동 실행
  - 반환형이 void -> 아무 일 안함 
  - 반환형이 객체 -> null 반환  

그래서 내 ReservationControllerTest에서 : 
```text
예외를 던질 거라고 설정했는데
실제로는 아무 예외도 안 던져짐
→ 컨트롤러는 정상 흐름 → 201 반환
```