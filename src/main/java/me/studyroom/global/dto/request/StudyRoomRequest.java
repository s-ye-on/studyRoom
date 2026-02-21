package me.studyroom.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;

public sealed interface StudyRoomRequest permits StudyRoomRequest.Create {

	record Create(
		@NotBlank (message = "스터디룸 이름 입력은 필수 입니다")
		String name,
		@NotNull (message = "이용 가능 여부 입력은 필수 입니다")
		boolean available,
		@NotBlank (message = "설명은 필수 입니다")
		String description,
		@NotNull (message = "영업시작 시간입력은 필수 입니다")
		LocalTime openTime,
		@NotNull (message = "영업종료 시간입력은 필수 입니다")
		LocalTime closeTime
	) implements StudyRoomRequest {}
}
