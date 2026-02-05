package me.studyroom.domain.reservation.policy;

import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.user.User;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class MinimumDurationPolicy implements ReservationPolicy {

	private static final Duration minimumDuration = Duration.ofHours(1);

	@Override
	public void validate(LocalDateTime start, LocalDateTime end, StudyRoom studyRoom, User user) {
		if (Duration.between(start, end).compareTo(minimumDuration) < 0) {
			throw new ReservationException(ExceptionCode.TOO_SHORT_RESERVATION);
		}
	}
}
