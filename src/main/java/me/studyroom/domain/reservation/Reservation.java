package me.studyroom.domain.reservation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.user.User;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "reservation",
	indexes = {
		@Index(
			name = "idx_reservation_room_status_time",
			columnList = "study_room_id, status, start_at, end_at"
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_room_id", nullable = false)
	private StudyRoom studyRoom;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Enumerated(EnumType.STRING) // @Enumerated는 Enum 타입 필드에만 적용해야 함
	@Column(nullable = false)
	private ReservationStatus status;

	public Reservation(User user, StudyRoom studyRoom, LocalDateTime startAt, LocalDateTime endAt) {
		this.user = user;
		this.studyRoom = studyRoom;
		this.startAt = startAt;
		this.endAt = endAt;
		this.status = ReservationStatus.WAIT_PAYMENT;
	}

	public void update(StudyRoom studyRoom, LocalDateTime startAt, LocalDateTime endAt) {
		this.studyRoom = studyRoom;
		this.startAt = startAt;
		this.endAt = endAt;
	}

	public void updateStudyRoom(StudyRoom studyRoom) {
		this.studyRoom = studyRoom;
	}

	public void updateStartAt(LocalDateTime startAt) {
		this.startAt = startAt;
	}

	public void updateEndAt(LocalDateTime endAt) {
		this.endAt = endAt;
	}

	public void confirm() {
		if (status != ReservationStatus.WAIT_PAYMENT) {
			throw new ReservationException(ExceptionCode.INVALID_STATUS);
		}
		this.status = ReservationStatus.CONFIRMED;
	}

	public void expire() {
		if (this.status == ReservationStatus.WAIT_PAYMENT) {
			this.status = ReservationStatus.EXPIRED;
		}
	}

	public void canceled() {
		if (status == ReservationStatus.EXPIRED) {
			throw new ReservationException(ExceptionCode.ALREADY_EXPIRED);
		}
		this.status = ReservationStatus.CANCELED;
	}
}

