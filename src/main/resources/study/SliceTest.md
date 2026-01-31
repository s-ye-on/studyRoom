# Slice Test (컨트롤러 테스트)

### ✔ 슬라이스 테스트의 철칙
- ❌실제 구현을 다 가져오지 않는다 
- ⭕️**컨트롤러가 의존하는 경계까지만 살린다**
- ⭕️그 안쪽은 전부 Mock


### 🔒보안 슬라이스 테스트의 베스트 프랙티스 정리 
선택지는 2개 

#### 🅰️ 진짜 보안 설정 켜고 싶을 때 (지금 해놓은 방식)
- `@AutoCOnfigureMockMvc (addFilters = true)`
- `@Import(securityConfig.class)`
- **SecurityConfig가 요구하는 Bean 전부 Mock**

#### 🅱️ 보안 로직 무시하고 컨트롤러만 볼 때 
- `@WebMvcTest`
- `.with(user())`
- SecurityConfig import ❌

## 목표 
### Controller 단위(Slice Test)에서 
- 인증된 사용자 정보를 주입받고
- 정상 / 예외 응답이 설계한 HTTP 계약대로 내려오는지 검증

즉, 
- ❌DB 안 씀
- ❌Service로직 검증 안함
- ⭕️Controller + Validation + Security + ExceptionHandler만 검증 

## Slice Test? 
### Slice Test 정의 
    Spring 애플리케이션의 일부 계층만 잘라서 테스트하는 방식  

대표적인 Slice Test : 
- @WebMvcTest -> Controller 계층
- @DataJpaTest -> Repository 계층
- @JsonTest -> 직렬화 / 역직렬화

### 왜 Slice Test를 쓰는가? 
| 이유        | 설명                    |
|-----------|-----------------------|
| 빠름        | Context가 작다           |
| 원인 추적 쉬움  | 깨지면 "여기 문제"           |
| 계약 검증에 최적 | HTTP status / JSON 구조 |

-> Controller 테스트에서 Slice Test가 정답 

---

## 3️⃣ WebMvcTest가 하는 일 
```java
@WebMvcTest(ReservationController.class)
```
이 한줄의 의미 : 
- ReservationController만 로딩 
- DispatcherServlet, Jackson, Validation 로딩
- ❌Service, Repository, SecurityConfig는 기본적으로 로딩 안함 
-> 그래서 직접 필요한 것들만 골라서 넣어줘야 함  

---
## 4️⃣보안이 들어오면서 복잡해진 이유 
### 컨트롤러 메서드 
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ReservationResponse.Create reserve(
    @Valid @RequestBody ReservationRequest.Create createRequest,
    @AuthenticationPrincipal CustomUserDetails user
) {
    return reservationService.reserve(createRequest, user.getId());
}
```
여기서 핵심 : 
- `@AuthenticationPrincipal`
- `CustomUserDetails`
- `user.getId()`  
👉**SecurityContext에 인증 객체가 없으면 컨트롤러 자체가 터진다**

---
## 5️⃣CustomUserDetails 설계 개념
```java
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
```
### ✔ 왜 User를 감싸는 구조인가? 
- Security는 UserDetails만 안다 
- 도메인 User는 Security에 종속되면 안된다 
- 그래서 **Security 전용 어댑터 객체**를 만들었다  
👉**도메인 보호 + 관심사 분리**

---
## 6️⃣Slice Test에서 Security를 살리는 방법 
### 두가지 선택지 
#### ❌보안 무시 
```java
.with(user("test"))
```
- 간단하지만
- 실제 Security 흐름 검증 ❌

#### ✅보안 포함 (내가 선택한 방식)
```java
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
```
👉**실제 SecurityFilterChain을 태운다**

---
## 7️⃣왜 테스트가 계속 깨졌나? 
### 핵심 원인 
```java
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
}
```
SecurityConfig는 생성될 때 이 **Bean을 요구**한다  

그런데 `@WebMvcTest`환경에선?
- ❌CustomUserDetailsService 자동 등록 안 됨 
- ❌그래서 ApplicationContext 로딩 실패 

---
## 8️⃣해결 방법 
### ✔ SecurityConfig가 요구하는 Bean을 Mock으로 제공 
```java
@MockitoBean
private CustomUserDetailsService customUserDetailsService;
```
이 한 줄 의미: 
- 실제 구현은 필요 없음
- **존재만 하면 됨**
- 테스트 중 호출 안 돼도 OK  

👉**Bean 존재 여부 ⭕️/ 동작 여부❌**

---
## 9️⃣SecurityContext를 직접 주입하는 이유 
### 테스트에서 인증 객체를 만드는 코드 
```java
private SecurityContext mockSecurityContext() {
    CustomUserDetails principal = mockUser();

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );

    SecurityContext context = new SecurityContextImpl();
    context.setAuthentication(auth);
    return context;
}
```
그리고 요청에 주입 
```java
mockMvc.perform(post("/reservations")
    .with(securityContext(mockSecurityContext()))
    ...
)
```
### 이게 의미하는 것  
"이 요청은 이미 인증된 사용자 요청이다"  

그래서 컨트롤러에서 
```java
@AuthenticationPrincipal CustomUserDetails user
```
가 **정상적으로 주입됨**

---
## 🔟Service는 왜 @MockitoBean인가? 
```java
@MockitoBean
private ReservationService reservationService;
```
- Controller 테스트에서 Service 로직은 관심이 없다 
- Controller가 **Service를 어떻게 호출하는지만** 중요 

그래서 
```java
given(reservationService.reserve(any(), anyLong()))
    .willReturn(response);
```
또는 
```java
doThrow(new ReservationException(...))
```
---
## 1️⃣1️⃣ 검증한 테스트 시나리오 
### ✅성공 케이스 
- 201 CREATED
- 응답 JSON 구조 확인

### ✅비즈니스 예외
- 예약 중복 -> 409 CONFLICT
- ErrorResponse 계약 검증 

### ✅Validation 오류 
- 요청 값 누락 -> 400 BAD REQUEST 
- fieldErrors 내려오는지 검증 

---
## 최종 한 줄 요약 
이 테스트는  
"예약 컨트롤러가 인증된 사용자 요청을 받아  
정상 / 예외 상황에서 HTTP 계약을 지키는지"를 검증하는  
Security 포함 Slice Test이다  
