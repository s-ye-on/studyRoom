package me.studyroom.domain.reservation;

public enum ReservationStatus {
	WAIT_PAYMENT, // 예약 생성 직후
	CONFIRMED, // 결제 완료

	RESERVED, // 임시 유지
	CANCELED, // 사용자 취소
	EXPIRED // 결제 시간 초과
}
