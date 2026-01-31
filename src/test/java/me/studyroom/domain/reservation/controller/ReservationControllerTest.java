package me.studyroom.domain.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.studyroom.config.SecurityConfig;
import me.studyroom.domain.reservation.dto.ReservationResponse;
import me.studyroom.domain.reservation.service.ReservationService;
import me.studyroom.domain.user.User;
import me.studyroom.global.dto.request.ReservationRequest;
import me.studyroom.global.exception.ApiExceptionHandlerV2;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;
import me.studyroom.security.CustomUserDetails;
import me.studyroom.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@AutoConfigureMockMvc(addFilters = true)
@WebMvcTest(ReservationController.class)
@Import({
	SecurityConfig.class,
	ApiExceptionHandlerV2.class,
	ReservationControllerTest.TestConfig.class
})
public class ReservationControllerTest {

	/*
	테스트 전용 Bean 분리
	운영 코드 영향 없음
	LocalDateTime 직렬화 해결
	실무에서도 그대로 쓰는 방식
	 */
	@TestConfiguration
	static class TestConfig {
		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder()
				.addModule(new JavaTimeModule())
				.build();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	// 기존 Spring Boot 2.x~3.x는 @MockBean
	// Spring Boot 4부터는 테스트 Mock 어노테이션이 변경됨
	// MockBean : 스프링이 만든 Mock MockitoBean : Mockito 기반으로 만든 Bean Override
	@MockitoBean
	private ReservationService reservationService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	// 테스트 유틸 메서드
	private CustomUserDetails mockUser() {
		User user = new User(
			"홍길동",
			"test@test.com",
			"password",
			"101012341234"
		);

		ReflectionTestUtils.setField(user, "id", 1L);

		return new CustomUserDetails(user);
	}

	// Authentication 생성 헬퍼 추가
	private UsernamePasswordAuthenticationToken mockAuthentication() {
		CustomUserDetails principal = mockUser();

		return new UsernamePasswordAuthenticationToken(
			principal,
			null,
			principal.getAuthorities()
		);
	}

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

	// 컨트롤러가 예외를 던졌을 때, HTTP 응답이 우리가 설계한 규격대로 내려오는지 검증한다
	// HTTP 계약(Contact) 검증
	@Test
	void 예약_중복시_에러응답_반환() throws Exception {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			1L,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(3)
		);

		doThrow(new ReservationException(ExceptionCode.SCHEDULE_CONFLICT))
			.when(reservationService)
			.reserve(any(), ArgumentMatchers.nullable(Long.class));

		// when, then
		mockMvc.perform(post("/reservations")
				.with(securityContext(mockSecurityContext()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.path").value("/reservations"));
	}

	@Test
	void 예약_성공_201_반환() throws Exception {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			1L,
			LocalDateTime.of(2026, 1, 10, 10, 0),
			LocalDateTime.of(2026, 1, 10, 11, 0)
		);

		ReservationResponse.Create response = new ReservationResponse.Create(
			"홍길동",
			"A룸",
			request.startAt(),
			request.endAt()
		);

		// 이 부분이 stub
		given(reservationService.reserve(any(), ArgumentMatchers.nullable(Long.class)))
			.willReturn(response);

		// when, then
		mockMvc.perform(post("/reservations")
				.with(securityContext(mockSecurityContext()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.reservationPersonName").value("홍길동"))
			.andExpect(jsonPath("$.reservationRoomName").value("A룸"))
			.andExpect(jsonPath("$.startAt").exists())
			.andExpect(jsonPath("$.endAt").exists());
	}

	@Test
	void 예약_요청값_누락시_400_반환() throws Exception {
		/*
		DTO에 @NotNull, @NotBlank 붙여놨는데 그게 실제로 400을 내리는지 검증
		1. 400 BadRequest
		2. ErrorResponse 구조 유지
		3. fieldErrors 내려오는지
		 */
		ReservationRequest.Create request = new ReservationRequest.Create(
			null, null, null
		);

		mockMvc.perform(post("/reservations") // 가짜 HTTP POST 요청 생성
				.with(securityContext(mockSecurityContext()))
				.contentType(MediaType.APPLICATION_JSON) // JSON 요청임을 명시
				.content(objectMapper.writeValueAsString(request))) // DTO를 JSON 문자열로 변환
			.andExpect(status().isBadRequest()) // HTTP 상태코드 검증
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST")) // 응답 JSON 필드 검증
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}
}
