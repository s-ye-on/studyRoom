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

		// 이 부분은 studyRoom에 묻는 것 같음
		// studyRoom.validateOperatingTime() 이런식으로 판단해라! 로 좀 더 객체지향적으로 만들어도 좋을 것 같음
//
		studyRoom.validateOperatingTime(startTime, endTime);
	}
}
