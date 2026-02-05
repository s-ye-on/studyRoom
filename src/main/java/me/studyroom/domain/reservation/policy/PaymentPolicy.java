package me.studyroom.domain.reservation.policy;

import me.studyroom.domain.reservation.Reservation;
import me.studyroom.domain.reservation.ReservationStatus;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.user.User;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;

import java.time.LocalDateTime;

public class PaymentPolicy implements ReservationPolicy {

	@Override
	public PolicyPhase phase() {
		return PolicyPhase.PAYMENT_CONFIRM;
	}

	@Override
	public void validate(LocalDateTime start,
											 LocalDateTime end,
											 StudyRoom studyRoom,
											 User user,
											 Reservation reservation) {

		// 결제 완료 전에 다시 중복 체크
		if (reservation.getStatus() != ReservationStatus.WAIT_PAYMENT) {
			throw new ReservationException(ExceptionCode.INVALID_STATUS);
		}
	}
}
