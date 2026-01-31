package me.studyroom.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public sealed interface ReservationRequest
	permits ReservationRequest.Create,
	ReservationRequest.Update,
	ReservationRequest.Delete {

	/*
	@NotBlank : String 전용
	@NotEmpty : String, Collection
	@NotNull : 모든 타입
	 */

	record Create(
		@NotNull(message = "스터디룸 선택은 필수입니다")
		Long studyRoomId,

		@NotNull(message = "시작 시간 선택은 필수입니다")
		LocalDateTime startAt,

		@NotNull(message = "종료 시간 선택은 필수입니다")
		LocalDateTime endAt

	) implements ReservationRequest {
	}

	record Update(
		@NotBlank(message = "비밀번호 입력은 필수입니다")
		String password,

		@NotNull(message = "스터디룸 선택은 필수입니다")
		Long StudyRoomId,

		@NotNull(message = "시작 시간 선택은 필수입니다")
		LocalDateTime startAt,

		@NotNull(message = "종료 시간 선택은 필수입니다")
		LocalDateTime endAt

	) implements ReservationRequest {
	}

	record Delete(
		@NotBlank(message = "비밀번호 입력은 필수입니다")
		String password
	) implements ReservationRequest {
	}
}
