//package me.studyroom.global.exception;
//
//import jakarta.validation.ConstraintViolationException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//// RestControllerAdvice가 v1, v2 두개 있으면 어떤게 먼저 실행될지 모르니 여긴 주석 처리하겠음
//@RestControllerAdvice
//public class ApiExceptionHandlerV1 {
//	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandlerV1.class);
//	/*
//	현재 v1에서는
//	- 응답이 그냥 String
//	- 에러 코드 없음
//	- timestamp 없음
//	- path 없음
//	- validation 에러 구조화 안됨
//	- 클라이언트가 에러를 기계적으로 처리하기 어려움
//
//	업그레이드해야 할 것
//	기존에서 "표준 에러 응답 포맷"을 반환하도록 업그레이드 해야한다"
//	 */
//
//	@ExceptionHandler(ApiException.class)
//	public ResponseEntity<?> handleApiException(ApiException e) {
//		var code = e.getExceptionCode();
//
//		log.warn("Custom Exception 발생 : {} ", code.getMessage());
//
//		return ResponseEntity
//			.status(code.getStatus())
//			.body(e.getMessage());
//	}
//
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	@ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
//	public String handleConstraintViolationException(Exception e) {
//		return e.getMessage();
//	}
//
//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//	@ExceptionHandler(Exception.class)
//	public String handlerUncaughtException(Exception e) {
//		log.error("Unexpected error : ", e);
//		return "서버 오류가 발생했습니다";
//	}
//}
