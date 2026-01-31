package me.studyroom.domain.reservation;

// 동시성 테스트
// 목표 : 동시에 2개의 예약 요청을 보내도, DB에는 예약이 1건만 저장되어야 한다

import me.studyroom.domain.reservation.service.ReservationService;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.dto.request.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReservationConcurrencyTest {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private StudyRoomRepository studyRoomRepository;

	@Autowired
	private UserRepository userRepository;

	private Long studyRoomId;
	private Long user1Id;

	@BeforeEach
	void setUp() {
		reservationRepository.deleteAll();
		studyRoomRepository.deleteAll();
		userRepository.deleteAll();

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
			LocalDateTime.of(2026, 1, 10, 10, 0),
			LocalDateTime.of(2026, 1, 10, 11, 0)
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
				e.printStackTrace();
				failCount.incrementAndGet();
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
	}

	@Test
	void 시간_경계_예약_성공() {
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.of(2026, 1, 10, 10, 0),
			LocalDateTime.of(2026, 1, 10, 11, 0)
		);
	}

	@Test
	void 시간_경계_겹칩_예약_실패() {
	}

	@Test
	void 취소된_예약_시간_예약_성공() {
	}

}
