# ErrorResponse로 예외 응답 포맷 통일하기 

## 🎯 목표
지금 이런 예외들이 섞여 있다
- ReservationException
- StudyRoomException
- Validation 에러 (@Valid)
- JSON 파싱 에러
- 타입 에러 등등

이걸 전부 아래처럼 **하나의 규격**으로 내려주고 싶다
```json
{
  "code": "SCHEDULE_CONFLICT",
  "message": "이미 예약되어 있는 시간대 입니다",
  "status": 409,
  "timestamp": "2026-01-21T01:20:30",
  "path": "/reservations"
}
```
또는 validation 에러라면: 
```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다",
  "errors": [
    { "field": "email", "message": "이메일 형식이 아닙니다" }
  ]
}
```
## ✅1단계 - ErrorResponse DTO 만들기 
먼저 공통 에러 응답 DTO부터 만들자 
```java
package me.studyroom.global.exception;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ErrorResponse(
    String code,
    String message,
    int status,
    LocalDateTime timestamp,
    String path,
    List<FieldError> errors
) {

    @Builder
    public record FieldError(
        String field,
        String message
    ) {
    }

    public static ErrorResponse of(
        ExceptionCode exceptionCode,
        String path
    ) {
        return ErrorResponse.builder()
            .code(exceptionCode.name())
            .message(exceptionCode.getMessage())
            .status(exceptionCode.getStatus().value())
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();
    }

    public static ErrorResponse ofValidation(
        String path,
        List<FieldError> errors
    ) {
        return ErrorResponse.builder()
            .code("INVALID_REQUEST")
            .message("요청 값이 올바르지 않습니다")
            .status(400)
            .timestamp(LocalDateTime.now())
            .path(path)
            .errors(errors)
            .build();
    }
}
```
### ✔ 핵심 포인트
- record -> 불변 DTO
- builder 사용 -> 가독성
- validation 에러 전용 팩토리 메서드 분리

### 필드 의미 

| 필드        | 의미                    |
|-----------|-----------------------|
| code      | 시스템 식별 코드 (enum name) |
| message   | 사용자 메시지               |
| status    | HTTP 상태 코드            |
| timestamp | 발생 시간                 |
| path      | 요청 URI                |
| errors    | 필드 검증 상세              |

### 왜 record + static factory 썼나? 
```java
public static ErrorResponse of(...)
```
#### 이유 
- 생성 로직 중앙화
- 필드 누락 방지
- 의미 있는 생성 방법 제공

---
## ✅2단계 - GlobalExceptionHandler 만들기
이제 모든 예외를 한 곳에서 처리하게 만들자 
```java
package me.studyroom.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 비즈니스 예외 (ApiException)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
        ApiException e,
        HttpServletRequest request
    ) {
        ExceptionCode code = e.getExceptionCode();

        log.warn("Business Exception: {}", code.name());

        ErrorResponse response = ErrorResponse.of(
            code,
            request.getRequestURI()
        );

        return ResponseEntity
            .status(code.getStatus())
            .body(response);
    }

    /**
     * @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException e,
        HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> errors =
            e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                    .field(error.getField())
                    .message(error.getDefaultMessage())
                    .build())
                .toList();

        ErrorResponse response = ErrorResponse.ofValidation(
            request.getRequestURI(),
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * PathVariable, RequestParam validation 에러
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException e,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
            .code("INVALID_REQUEST")
            .message(e.getMessage())
            .status(400)
            .timestamp(java.time.LocalDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 최후의 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception e,
        HttpServletRequest request
    ) {
        log.error("Unexpected error", e);

        ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("서버 내부 오류가 발생했습니다")
            .status(500)
            .timestamp(java.time.LocalDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.internalServerError().body(response);
    }
}
```
ApiExceptionHandler를 리팩터링했다 

---
## 3단계 - 제거해야 할 것들 

❌`@ResponseStatus`  
-> ResponseEntity로 status 제어하므로 제거  

❌`String 반환`
-> 항상 ErrorResponse 반환  

❌`메시지 직접 반환`  
-> 클라이언트는 구조화된 JSON만 받음

---
## 리팩터링해서 얻는 효과
### ✔API 응답 일관성 확보 
모든 에러가 같은 구조 : 
```json
{
  "code": "...",
  "message": "...",
  "status": 400,
  "timestamp": "...",
  "path": "...",
  "errors": []
}
```
---
### ✔ 프론트/ 클라이언트가 처리하기 쉬움
```js
if (error.code === "SCHEDULE_CONFLICT") {
   showToast(error.message)
}
```
---
### ✔ 테스트 작성도 쉬워짐
```java
.andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
.andExpect(jsonPath("$.status").value(409))
```
---
## 코드 설명
### code.name() 하면 뭐가 나오나
ExceptionCode는 enum이다
```java
public enum ExceptionCode {
    INVALID_TIME_RANGE,
    NOT_FOUND_USER,
    SCHEDULE_CONFLICT
}
```
자바 enum에는 기본 메서드로 code.name()이 있음  
-> **선언된 상수 이름 그대로 문자열 반환**  

예시 : 
```java
ExceptionCode.SCHEDULE_CONFLICT.name()
// 결과: "SCHEDULE_CONFLICT"
```
왜 굳이 name()을 쓰나?  
```json
{
  "code": "SCHEDULE_CONFLICT",
  "message": "이미 예약되어 있는 시간대 입니다"
}
```
- 프론트에서 분기 처리하기 쉬움
- 다국어 처리도 쉬움 (code 기준으로 번역)
- 로그 분석에도 좋음
- 메시지가 바뀌어도 code는 안정적  
즉, message= 사람용, code는 시스템용

---
### e.getBindingResult()는 뭔가? 
이건 **@Valid 검증 실패 시에만 등장하는 객체**  
예를 들어 DTO:
```java
public record Create(
    @NotBlank String name,
    @Email String email
) {}
```
컨트롤러: 
```java
@PostMapping
public void create(@Valid @RequestBody Create request) { ... }
```
만약 클라이언트가: 
```json
{
  "name": "",
  "email": "abc"
}
```
를 보낸다면?  
Spring이 자동으로 검증하고  
-> 실패하면 MethodArgumentNotValidException 발생  

이 예외 안에 들어있는 정보 
```java
e.getBindingResult()
```
여기에는 : 
- 어떤 필드가 실패했는지
- 어떤 메시지인지
- 어떤 값이 들어왔는지  
모든 검증 결과가 들어 있다 

예시 : 
```java
e.getBindingResult().getFieldErrors()
```
결과:  

| field | message      |
|-------|--------------|
| name  | 공백일 수 없습니다   |
| email | 이메일 형식이 아닙니다 |

나는 이렇게 변환했었다 : 
```java
.map(error -> new ErrorResponse.FieldError(
    error.getField(),
    error.getDefaultMessage()
))
```
그래서 응답이 : 
```json
"errors": [
  { "field": "name", "message": "공백일 수 없습니다" },
  { "field": "email", "message": "이메일 형식이 아닙니다" }
]
```
`BindingResult`는 검증 실패 상세 정보 묶음 객체

---
## 추가적으로 궁금했던 것 
아래 3개는 전부 같은 동작 
```java
ResponseEntity.badRequest().body(response);
```
```java
ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
```
```java
new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
```
👉단지 가독성과 편의성 차이일 뿐