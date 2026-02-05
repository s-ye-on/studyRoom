package me.studyroom.domain.reservation.policy;

import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.user.User;

import java.time.LocalDateTime;

public class PaymentPolicy implements ReservationPolicy {
	@Override
	public void validate(LocalDateTime start, LocalDateTime end, StudyRoom studyRoom, User user) {
		/// todo : 결제 모듈 붙으면 구현
		/// PaymentPolicy 만들면 상태 전이도 만들어야 함 RESERVED -> WAIT_PAYMENT ->CONFIRMED
	}
}
