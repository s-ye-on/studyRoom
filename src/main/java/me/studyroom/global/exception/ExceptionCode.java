package me.studyroom.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {

	// 400 입력값이 유효하지 않음(요청 데이터 문제)
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
	INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "유효하지 않은 시간 요청입니다"),
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일이 일치하지 않습니다"),
	OUT_OF_OPERATING_TIME(HttpStatus.BAD_REQUEST, "운영 시간이 아닙니다"),
	INVALID_OPERATING_TIME(HttpStatus.BAD_REQUEST, "운영 시간 입력이 바르지 않습니다"),


	// 404
	NOT_FOUND_STUDYROOM(HttpStatus.NOT_FOUND, "존재하지 않는 스터디룸입니다"),
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 아이디입니다"),
	NOT_FOUND_RESERVATION(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다"),

	//405 메서드를 수행하기 위한 해당 자원이 이용 불가일 때
	STUDYROOM_NOT_AVAILABLE(HttpStatus.METHOD_NOT_ALLOWED, "현재 이용할 수 없는 스터디룸입니다"),

	// 409 conflict 요청은 유효하지만, 현재 상태에서 수행할 수 없음
	SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "이미 예약되어 있는 시간대 입니다");

	private final HttpStatus status;
	private final String message;

}
