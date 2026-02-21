package me.studyroom.domain.reservation.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.reservation.ReservationRepository;
import me.studyroom.domain.reservation.ReservationStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

	private static final int PAYMENT_TIMEOUT_MINUTES = 10;

	private final ReservationRepository reservationRepository;
	private final Clock clock;

	// 일단 1분마다 실행으로 주기를 둠 (나중에 원하는 주기로 바꾸겠다)
	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void expiredWaitPayments() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime deadline = now.minusMinutes(PAYMENT_TIMEOUT_MINUTES);

		int updated = reservationRepository.expireWaitPayments(
			ReservationStatus.WAIT_PAYMENT,
			ReservationStatus.EXPIRED,
			deadline
		);

		// 로그
		// log.info("Expired WAIT_PAYMENT reservations: {}", updated);
	}
}
