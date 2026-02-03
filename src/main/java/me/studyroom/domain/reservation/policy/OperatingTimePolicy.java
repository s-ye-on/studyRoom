package me.studyroom.domain.reservation.policy;

import lombok.RequiredArgsConstructor;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
@Component
public class OperatingTimePolicy implements ReservationPolicy {

	@Override
	public void validate(LocalDateTime start, LocalDateTime end, StudyRoom studyRoom) {
		LocalTime startTime = start.toLocalTime();
		LocalTime endTime = end.toLocalTime();

		if(!studyRoom.isWithinOperatingTime(startTime, endTime)) {
			throw new ReservationException(ExceptionCode.OUT_OF_OPERATING_TIME);
		}
	}
}
