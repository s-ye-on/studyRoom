# 도메인 객체와 Security 객체를 분리하기 
## 왜 분리해야 하나? 
```java
@AuthenticationPrincipal User user   // ← 도메인 엔티티
```
이렇게 **보안 계층이 도메인 엔티티에 직접 의존**하고 있다  

이건 위험할 수도 있다 

### ❌문제점
1. 보안 프레임워크가 도메인을 오염시킴
    - Spring Security 요구사항(UserDetails, 권한, 계정 상태 등)이
    - 도메인 모델 설계에 섞임
2. 테스트가 어려워짐
    - Security Mock은 `org.springframework.security.core.userdetails.User`를 사용
    - 도메인 User와 타입이 다르다 -> 매칭 문제 발생
3. 확장 시 유지보수 지옥
    - OAuth, JWT, 소셜 로그인 붙이면
    - 도메인 User 구조 계속 흔들림
  
그래서 실무에서는 거의 무조건 :  
👉 **Security 전용 UserPrincipal 객체를 따로 둔다**

---
## ✅정석 구조 
```java
[Domain]
  User (엔티티)

[Security]
  CustomUserPrincipal implements UserDetails

[Controller]
  @AuthenticationPrincipal CustomUserPrincipal principal
```
---
## ✨예시로 만들어보자 
### 1️⃣Security 전용 Principal 만들기 
```java
package me.studyroom.security;

import me.studyroom.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;

    public CustomUserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
    }

    public Long getUserId() {
        return userId;
    }

    // ===== UserDetails 구현 =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // 지금은 권한 없으면 비워도 OK
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```
## CustomUserDetails vs CustomUserDetailsService
Spring Security 인증 흐름
```java
[로그인 요청]
      ↓
AuthenticationFilter
      ↓
UserDetailsService.loadUserByUsername()
      ↓
CustomUserDetails 생성
      ↓
SecurityContext 에 저장
      ↓
Controller에서 @AuthenticationPrincipal 로 꺼내씀
```

| 구성요소                     | 역할                                              |
|--------------------------|-------------------------------------------------|
| CustomUserDetails        | 로그인한 사용자 정보를 담는 **데이터 객체** (VO)                 |
| CustomUserDetailsService | DB에서 유저 조회해서 CustomUserDetails 만들어주는 **조회 서비스** |

### 1️⃣ CustomUserDetails는 그냥 "보안용 DTO"이다 
```java
public class CustomUserDetails implements UserDetails {
    private final User user;

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }
}
```
이 객체는 :  
✔ 데이터 보관  
✔ Security가 요구하는 인터페이스 구현  
✔ Controller / Service에서 사용자 정보 꺼내기  
만 담당  

❌DB 조회하지 않는다 
❌비즈니스 로직 없다  
❌Service 필요 없다  

그냥 **보안 전용 래퍼 객체**  
DTO랑 거의 동일한 역할이라고 보면 된다  

---
### 2️⃣CustomUserDetailsService는 "조회 담당자"
Spring Security는 로그인할 때 내부적으로 무조건 이 메서드를 호출한다 : 
```java
UserDetails loadUserByUsername(String username)
```
그래서 우리가 구현해야 한다 :  
```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }
}
```
이 서비스의 역할 : 
✔ DB에서 User 조회  
✔ User -> CustomUserDetails 변환  
✔ Security에게 전달  

--- 
## 비유 
은행 창구 비유

| 역할                       | 의미                     |
|--------------------------|------------------------|
| CustomUserDetails        | 통장 (내 정보가 담긴 종이)       |
| CustomUserDetailsService | 은행 창구 직원 (통장 만들어주는 사람) |
| UserRepository           | 금고                     |
| SecurityContext          | 내 지갑                   |

---
### CustomUserDetails 안에 정보 가져오는 메서드 다 있는데 굳이 service가 필요한가? 
그 메서드들은 **이미 만들어진 객체에서 꺼내는 용도**이다  
**"객체를 생성하는 책임"**은 Service가 가져야 한다  

생성과 사용은 분리해야 한다 (SRP)

---
### 좋은 구조 예시
```text
[로그인 요청]
   ↓
CustomUserDetailsService
   ↓  (DB 조회)
User
   ↓
CustomUserDetails 생성
   ↓
SecurityContext 저장
   ↓
Controller
   ↓
principal.getId()
```
---
### 🚫흔한 잘못된 구조 (피해야함)
```java
CustomUserDetails {
    UserRepository repository; ❌
}
```
이렇게 되면:
- 객체가 DB 의존
- 테스트 어려움
- 책임 혼란
- 도메인 침범

**customUserService는 생성 책임 때문에 필요하다**

---
## 마지막 요약
| 구분                       | 역할                                            |
|--------------------------|-----------------------------------------------|
| CustomUserDetails        | 로그인 사용자 정보 담는 객체                              |
| CustomUserDetailsService | DB에서 유저 조회해서 CustomUserDetails 생성             |
| Servcie 필요?              | CustomUserDetails❌/ CustomUserDetailsService✅ |

---
## SpringConfig 등록 방법 
이제 이 서비스가 Spring Security 이늦ㅇ에 실제로 사용되도록 연결해야 한다 

### 기본 SecurityConfig 예시 
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup").permitAll()
                .anyRequest().authenticated()
            )
            .userDetailsService(customUserDetailsService) // ⭐ 핵심
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            );

        return http.build();
    }
}
```
🔥핵심 포인트 
```java
.userDetailsService(customUserDetailsService)
```
이 한줄로 :  
"로긍니 시 사용자 조회는 내가 만든 CustomUserDetailsService를 사용해라" 라고 Security에 알려주는 것 