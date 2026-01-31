package me.studyroom.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
	private final ExceptionCode exceptionCode;

	public ApiException(ExceptionCode exceptionCode, String message) {
		super(message);
		this.exceptionCode = exceptionCode;
	}

	public ApiException(ExceptionCode code) {
		this(code, code.getMessage());
	}
}
