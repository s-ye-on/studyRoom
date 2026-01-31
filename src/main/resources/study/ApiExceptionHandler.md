# ApiExceptionHandler 

## 전체 흐름 
```java
Controller
   ↓
Service
   ↓
Exception 발생
   ↓
@RestControllerAdvice (ApiExceptionHandler)
   ↓
HTTP Response 변환
```
## 1. ApiException
```java
public class ApiException extends RuntimeException {
    private final ExceptionCode exceptionCode;
}
```
의미 : 
 - ✔ 모든 비즈니스 예외의 공통 부모
 - ✔ HTTP 상태 코드 + 메시지를 함께 보관

---

## 2. @RestControllerAdvice
```java
@RestControllerAdvice
public class ApiExceptionHandler {
}
```
이건 : 
- 👉**전역 예외 처리기**  
컨트롤러에서 발생한 예외를  
여기서 가로채서 HTTP 응답으로 변환해줌

---

## 3. ApiException 처리
```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<?> handleApiException(ApiException e)
```
의미 : 
- ApiException이 발생하면
- 이 메서드가 자동 실행됨
- HTTP Status + 메시지를 응답으로 만들어줌

---

## 4. Validation 예외 처리
```java
@ExceptionHandler({
    ConstraintViolationException.class,
    MethodArgumentNotValidException.class
})
```
- `@Valid` 검증 실패 시 발생하는 예외들
- 400 Bad Request로 변환

---

## 5. RuntimeException 처리 (Fallback) 
```java
@ExceptionHandler(RuntimeException.class)
```
이건 마지막 안전망  
너무 광범위해서 실무에서는 조심해야 한다  

보통은 : 
```java
Exception.class
```
로 잡고,  
사용자에게는 일반 메시지,  
로그는 상세 출력  

---

## 🎯예외 구조 핵심 요약
| 역할                   | 의미               |
|----------------------|------------------|
| ApiException         | 비즈니스 예외 공통 부모    |
| ExceptionCode        | HTTp 상태 + 메시지    |
| RestControllerAdvice | 전역 예외 변환기        |
| ExceptionHandler     | 예외 -> HTTP 응답 매핑 |


