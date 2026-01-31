package me.studyroom.domain.reservation.dto;

import java.time.LocalDateTime;

public sealed interface ReservationResponse
	permits ReservationResponse.Create,
	ReservationResponse.Update,
	ReservationResponse.Read {

	record Create(
		String reservationPersonName,
		String reservationRoomName,
		LocalDateTime startAt,
		LocalDateTime endAt
	) implements ReservationResponse {
	}

	record Update(
		String reservationPersonName,
		String reservationRoomName,
		LocalDateTime startAt,
		LocalDateTime endAt
	) implements ReservationResponse {
	}

	record Read(
		String reservationPersonName,
		String reservationRoomName,
		LocalDateTime startAt,
		LocalDateTime endAt
	) implements ReservationResponse {
	}
}
