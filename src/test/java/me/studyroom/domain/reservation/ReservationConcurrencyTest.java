package me.studyroom.domain.reservation;

// 동시성 테스트
// 목표 : 동시에 2개의 예약 요청을 보내도, DB에는 예약이 1건만 저장되어야 한다

import jakarta.transaction.Transactional;
import me.studyroom.domain.reservation.service.ReservationService;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.dto.request.ReservationRequest;
import me.studyroom.global.exception.ReservationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// 동시성 + DB 결과 검증 테스트에서는 @Transactional 사용 금지
// 락이 실제로 DB에 반영됐는지 보는 테스트도 사용 금지
/*
@Transactional을 테스트에 붙이면
테스트 스레드에만 트랜잭션이 생기고
멀티스레드에서 실행되는 서비스 로직은
각각 별도의 트랜잭션으로 커밋된다.
하지만 테스트 스레드는
자기 트랜잭션 스냅샷만 보기 때문에
다른 스레드의 커밋 결과를 관찰할 수 없다.
 */
public class ReservationConcurrencyTest {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private StudyRoomRepository studyRoomRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean // 기존 TimeConfig의 Clock을 교체
	private Clock clock;

	private Long studyRoomId;
	private Long user1Id;

	private LocalDateTime BASE_TIME;

	@BeforeEach
	void setUp() {
		reservationRepository.deleteAll();
		studyRoomRepository.deleteAll();
		userRepository.deleteAll();

		Instant fixedInstant = Instant.parse("2026-10-01T00:00:00Z");

		Mockito.when(clock.instant()).thenReturn(fixedInstant);
		Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());

		BASE_TIME = LocalDateTime.now(clock);

		StudyRoom studyRoom = new StudyRoom("A룸", true, "테스트룸");
		studyRoomRepository.save(studyRoom);
		studyRoomId = studyRoom.getId();

		User user1 = new User("user1", "u1.test.com", "1234", "01011112222");
		User user2 = new User("user2", "u2.test.com", "1234", "01022223333");
		userRepository.saveAll(List.of(user1, user2));
		user1Id = user1.getId();
	}

	@Test
	void 동시에_예약시_하나만_성공() throws Exception {

		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

		CountDownLatch readyLatch = new CountDownLatch(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		ReservationRequest.Create createRequest = new ReservationRequest.Create(
			studyRoomId,
			BASE_TIME.plusHours(1),
			BASE_TIME.plusHours(2)
		);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		Runnable task = () -> {
			try {
				readyLatch.countDown(); // 준비 완료
				startLatch.await(); // 동시에 시작 대기

				reservationService.reserve(createRequest, user1Id);
				successCount.incrementAndGet();

			} catch (Exception e) {
				if (e instanceof ReservationException) {
					failCount.incrementAndGet(); // 의도된 동시성 실패
				} else {
					throw new RuntimeException(e); // 예상 못한 실패는 테스트 실패
				}
			} finally {
				doneLatch.countDown();
			}
		};

		for (int i = 0; i < threadCount; i++) {
			executorService.submit(task);
		}

		readyLatch.await(); // 모든 스레드 준비 대기
		startLatch.countDown(); // 동시에 출발
		doneLatch.await(); // 모두 종료 대기

		List<Reservation> reservations = reservationRepository.findAll();

		System.out.println("성공 = " + successCount.get());
		System.out.println("실패 = " + failCount.get());
		System.out.println("DB 예약 개수 = " + reservations.size());

		assertThat(reservations.size()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(1);
	}

	@Test
	void 시간_경계_예약_성공() {
		// 아직 테스트 미완성
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			BASE_TIME.plusHours(1),
			BASE_TIME.plusHours(2)
		);
	}

	@Test
	void 시간_경계_겹칩_예약_실패() {
	}

	@Test
	void 취소된_예약_시간_예약_성공() {
	}

}
