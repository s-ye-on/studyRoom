package me.studyroom.domain.studyRoom;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.StudyRoomException;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50, unique = true)
	private String name;

	@Getter(AccessLevel.NONE) // 특정 필드만 getter 비활성화, 외부에서 상태 직접 조회 불가
	// 반드시 ensureAvailable()를 통해서만 접근 (캡슐화)
	// available은 내부상태, 외부에서는 사용 가능한지 묻지말고 사용 가능해야 한다라고 요청하게 만듬
	// 도메인 규칙에 직접 영향을 주는 상태만 숨기는게 핵심
	// 이런 값들은 외부 if분기 유도, 규칙이 흩어질 가능성, 객체 무결성 깨질 가능성이 있기에 행동으로만 접근하게 만들어봄
	@Column(nullable = false)
	private boolean available; // studyRoom이 고장났을 수도 있으니. 시간 관리는 reservation쪽에서 하는게 맞음

	@Column(nullable = false, length = 100)
	private String description;

	@Column (nullable = false)
	private LocalTime openTime;

	@Column (nullable = false)
	private LocalTime closeTime;

	public StudyRoom(String name, boolean available, String description, LocalTime openTime, LocalTime closeTime) {
		validateOperatingTime(openTime, closeTime);

		this.name = name;
		this.available = available;
		this.description = description;
		this.openTime = openTime;
		this.closeTime = closeTime;
	}

	// 기본 규칙은 엔티티에서 지키도록 함 (스터디룸 자체의 불변식)
	// 엔티티 검증은 데이터 무결성
	// Policy는 예약 가능 여부
	private void validateOperatingTime(LocalTime openTime, LocalTime closeTime) {
		if(!openTime.isBefore(closeTime)) {
			throw new StudyRoomException(ExceptionCode.INVALID_OPERATING_TIME);
		}
	}

	// 상태 전이 규칙이 나중에 생길 수 있기에 if문으로 확인 후 변경으로 만들었다
	// 의미 없는 상태 변경 방지 (로그/ 이벤트/ 감사 기록이 꼬일 수 있음)
	public void enable() {
		if (this.available) {
			return;
		}
		this.available = true;
	}

	public void disable() {
		if (!this.available) {
			return;
		}
		this.available = false;
	}

	public void ensureAvailable() {
		if (this.available) {
			return;
		}
		throw new StudyRoomException(ExceptionCode.STUDYROOM_NOT_AVAILABLE);
	}

	// studyRoom은 정보 제공만
	public boolean isWithinOperatingTime(LocalTime start, LocalTime end) {
		return !start.isBefore(openTime) && !start.isAfter(end);
	}
}
