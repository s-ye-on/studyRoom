package me.studyroom.domain.reservation.policy;

import me.studyroom.domain.studyRoom.StudyRoom;

import java.time.LocalDateTime;


public interface ReservationPolicy {

	void validate(LocalDateTime start, LocalDateTime end, StudyRoom studyRoom);
}
