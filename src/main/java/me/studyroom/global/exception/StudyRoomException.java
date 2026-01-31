package me.studyroom.global.exception;

public class StudyRoomException extends ApiException {
	public StudyRoomException(ExceptionCode exceptionCode, String message) {
		super(exceptionCode, message);
	}

	public StudyRoomException(ExceptionCode code) {
		super(code);
	}
}
