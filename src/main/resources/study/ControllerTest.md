# Controller Test
## ReservationControllerTest
```java
@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

}
```
- `@WebMvcTest`
  - Controller, Jackson, ExceptionHandler만 로딩
  - DB, JPA, Service 실제 구현 안 뜸 
- `@MockBean ReservationService`
  - Controller가 의존하는 Service 가짜로 주입

---
## 비즈니스 예외 응답 테스트 만들기 
### 🎯목표
예약 충돌 예외가 발생하면  
-> HTTP 409  
-> ErrorResponse JSON 구조가 맞는지 검증

### 테스트 코드 예시 
```java
@Import(ApiExceptionHandlerV2.class)
@WebMvcTest(ReservationController.class)
public class ReservationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  // 기존 Spring Boot 2.x~3.x는 @MockBean
  // Spring Boot 4부터는 테스트 Mock 어노테이션이 변경됨
  // MockBean : 스프링이 만든 Mock MockitoBean : Mockito 기반으로 만든 Bean Override
  @MockitoBean
  private ReservationService reservationService;

  // 컨트롤러가 예외를 던졌을 때, HTTP 응답이 우리가 설계한 규격대로 내려오는지 검증한다
  // HTTP 계약(Contact) 검증
  @Test
  void 예약_중복시_에러응답_반환() throws Exception {
    // given
    ReservationRequest.Create request = new ReservationRequest.Create(1L, null, null);

//		given(reservationService.reserve(any(), anyLong()))
//			.willThrow(new ReservationException(ExceptionCode.SCHEDULE_CONFLICT));

    doThrow(new ReservationException(ExceptionCode.SCHEDULE_CONFLICT))
            .when(reservationService)
            .reserve(any(), ArgumentMatchers.nullable(Long.class));

    // when, then
    mockMvc.perform(post("/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.path").value("/reservations"));

  }
}
```
### 핵심 포인트 
#### ✔ @WebMbcTest(ReservationController.class
이 어노테이션은 : 
- Controller만 띄운다
- Service, Repository, DB 전부 안 띄운다 
- MVC 레이어만 테스트한다  
"스프링 전체 띄우지 말고, 웹 레이어만 빠르게 테스트하자"
  - 그래서 테스트가 아주 빠름

#### ✔ @Import(ApiExceptionHandlerV2.class)
이게 없으면: 
- ❌예외 핸들러가 컨텍스트에 등록되지 않음
- ❌예외 발생 시 Spring 기본 에러 응답으로 떨어짐  
우리가 만든 ResponseEntity식으로 나오는지 형태를 검증할 수 없었음

#### ✔ @MockitoBean
의미 : "실제 ReservationService 대신 Mockito Mock 객체를 스프링 빈으로 주입해라"  
그래서 Controller는:  
```text
Controller → (Mock Service) → 우리가 설정한 동작
```
으로 동작함  
DB ❌  
트랜잭션 ❌  
실제 로직 ❌  

완전 격리 테스트 

#### ✔ 예외를 강제로 발생 시키는 부분 
```java
doThrow(new ReservationException(ExceptionCode.SCHEDULE_CONFLICT))
    .when(reservationService)
    .reserve(any(), ArgumentMatchers.nullable(Long.class));
```
"`reservationService.reserve(...)`가 호출되면 무조건 예외를 던져라"  

실제 로직이 아니라  
**에러 상황을 인위적으로 만들어서 컨트롤러만 검증**하는 것  

Controller 테스트의 핵심 패턴

#### ✔ MockMvc - 가짜 HTTP  요청 보내기 
```java
mockMvc.perform(
    post("/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(new ObjectMapper().writeValueAsString(request))
)
```
이건 실제 서버를 띄우지 않고 : 
- 가짜 HTTP POST 요청을 컨트롤러 보냄
- JSON Body 포함
- Spring MVC 흐름 그대로 실행  

거의 실제 API 호출과 동일한 시뮬레이션

#### 응답 검증 
```java
.andExpect(status().isConflict())
.andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
.andExpect(jsonPath("$.status").value(409))
.andExpect(jsonPath("$.path").value("/reservations"));
```
| 검증 대상    | 의미              |
|----------|-----------------|
| status   | HTTP 상태 코드      |
| $.code   | JSON 필드         |
| $.status | 응답 바디 내부 status |
| $.path   | 요청 경로           |

즉, "API 스펙이 깨지지 않았는지 자동으로 보증한다"

#### ✔ mockMvc.perform(...)
-> 실제 HTTP 요청을 보내는 것처럼 테스트

#### ✔ given(...).willThrow(...)
-> Service가 예외를 던지도록 조작

#### ✔ jsonPath(...)
-> 응답 JSON 구조 검증

--- 
## Controller 테스트는 무엇을 테스트하나? 
### ✅테스트 대상
✔ URL 매핑  
✔ HTTP Method  
✔ Request Body 매핑  
✔ Validation 동작  
✔ Exception -> Response 변환  
✔ Status Code  
✔ Response JSON 구조  

### ❌테스트하지 않는 것 
❌비즈니스 로직  
❌DB 저장 여부  
❌트랜잭션  
❌락  
❌성능  

그건 Service/Integration 테스트 몫  

모든 실패 케이스를 Controller 테스트로 다 만들 필요는 없다  
HTTP 계약이 깨질 위험이 있는 대표 케이스만 골라서 테스트한다 

---
## Controller 테스트에서 반드시 필요한 것들 
### 1. 정상 요청 성공 케이스 (1개 이상)
```text
POST /reservations
→ 201 Created
→ 응답 JSON 구조 검증
```
API가 살아있다는 보증이라 필수 

### 2. Validation 실패 대표 케이스 (1~2개)
```text
startAt = null
→ 400
→ fieldErrors 존재
```
모든 필드 조합을 테스트할 필요는 없음  
"Validation이 HTTP 400으로 떨어진다"만 보증하면 충분  

### 3. 비즈니스 예외 대표 케이스 (1~2개)
```text
SCHEDULE_CONFLICT
→ 409
→ code, status, path 검증
```
다른 예외들(NOT_FOUND, INVALID_TIME_RANGE 등)은  
**Service 테스트에서 이미 검증했으니까 굳이 Controller에서 반복 안해도 됨**

### 4. 인증 실패 (선택)
보안이 중요하다면  
```text
인증 안 된 요청 → 401
```

