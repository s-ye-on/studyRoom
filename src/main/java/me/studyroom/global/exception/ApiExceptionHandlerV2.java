package me.studyroom.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandlerV2 {
	// 비즈니스 예외
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
		ExceptionCode code = e.getExceptionCode();

		log.warn("Business Exception 발생 : {} ", code.name());

		ErrorResponse response = ErrorResponse.of(code, request.getRequestURI());

		return ResponseEntity
			.status(code.getStatus())
			.body(response);
	}

	// @Valid 검증 실패
	// @Valid 검증 실패는 "비즈니스 예외"가 아니라 "요청 검증 예외"이기 때문에
	// ExceptionCode(HttpStatus 매핑)를 쓰지 않는다
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException e, HttpServletRequest request) {
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
			request.getRequestURI(), errors
		);

		// .badRequest()에서 400을 반환함
		return ResponseEntity.badRequest().body(response);
	}

	// PathVariable, RequestParam validation 에러
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
		ConstraintViolationException e,
		HttpServletRequest request
	) {
		ErrorResponse response = ErrorResponse.builder()
			.code("INVALID_REQUEST")
			.message(e.getMessage())
			.status(HttpStatus.BAD_REQUEST.value()) // 400
			.timestamp(java.time.LocalDateTime.now())
			.path(request.getRequestURI())
			.build();

		return ResponseEntity.badRequest().body(response);
	}

	/*
	왜 @ExceptionHandler(Exception.class)인가?
	@ExceptionHandler(HttpStatus.INTERNAL_SERVER_ERROR.class)
	이건 불가능하다. HTTP Status는 예외가 아니다
	@ExceptionHandler(Exception.class) 이 뜻은 : 모든 예외의 최상위 부모를 잡겠다
	우리가 예상한 예외 -> ApiException
	검증 예외 -> MethodArgumentNotValidException
	그 외 모든 예상 못한 예외 -> Exception.class
	 */
	// 최후의 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
		log.error("Unexpected error", e);

		ErrorResponse response = ErrorResponse.builder()
			.code("INTERNAL_SERVER_ERROR")
			.message("서버 내부 오류가 발생했습니다")
			.status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
			.timestamp(java.time.LocalDateTime.now())
			.path(request.getRequestURI())
			.build();

		return ResponseEntity.internalServerError().body(response);
	}
}
