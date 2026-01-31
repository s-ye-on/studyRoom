package me.studyroom.global.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ErrorResponse(
	String code,
	String message,
	int status,
	LocalDateTime timestamp,
	String path,
	List<FieldError> fieldErrors // 상황별로 필요한 필드만 채우도록 builder 패턴을 사용
) {

	// 하나의 입력값 검증 실패 정보를 표현하는 DTO
	// 어떤 필드가 어떤 이유로 검증에 실패했는지를 담음
	@Builder
	public record FieldError(String field, String message) {
	}

	// 비즈니스 예외용
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
			.fieldErrors(List.of()) // 빈리스트로 채워줘서 null-safe 처리, 실무에서는 빈 리스트 방식이 더 많이 쓰임
			.build();
	}

	// 검증 에러용 @Valid
	public static ErrorResponse ofValidation(
		String path,
		List<FieldError> errors
	) {
		return ErrorResponse.builder()
			.code("INVALID_REQUEST")
			.message("요청 값이 올바르지 않습니다")
			.status(HttpStatus.BAD_REQUEST.value()) //400
			.timestamp(LocalDateTime.now())
			.path(path)
			.fieldErrors(errors)
			.build();
	}
}
