package me.studyroom.global.exception;

public class ReservationException extends ApiException {
	public ReservationException(ExceptionCode code, String message) {
		super(code, message);
	}

	public ReservationException(ExceptionCode code) {
		super(code);
	}
}
