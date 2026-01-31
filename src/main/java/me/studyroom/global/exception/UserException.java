package me.studyroom.global.exception;

public class UserException extends ApiException {
	public UserException(ExceptionCode code) {
		super(code);
	}

	public UserException(ExceptionCode code, String message) {
		super(code, message);
	}
}
