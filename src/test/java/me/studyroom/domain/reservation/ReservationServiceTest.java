package me.studyroom.domain.reservation;

import jakarta.transaction.Transactional;
import me.studyroom.domain.reservation.service.ReservationService;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.dto.request.ReservationRequest;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;
import me.studyroom.global.exception.StudyRoomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class ReservationServiceTest {
	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StudyRoomRepository studyRoomRepository;

	private Long studyRoomId;
	private Long user1Id;
	private Long user2Id;

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
		user2Id = user2.getId();
	}

	@Test
	void 시간_경계_예약_성공() {
		//given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(2)
		);

		ReservationRequest.Create request2 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(2),
			LocalDateTime.now().plusHours(3)
		);

		//when
		reservationService.reserve(request1, user1Id);
		reservationService.reserve(request2, user1Id);

		//then
		assertThat(reservationRepository.findAll()).hasSize(2);
	}

	@Test
	void 시간_경계_겹칩_예약_실패() {
		//given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(2),
			LocalDateTime.now().plusHours(3)
		);

		ReservationRequest.Create request2 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(3)
		);

		//when, then
		reservationService.reserve(request1, user1Id);

		assertThatThrownBy(() -> reservationService.reserve(request2, user1Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.SCHEDULE_CONFLICT.getMessage());

		assertThat(reservationRepository.findAll()).hasSize(1);
	}

	@Test
	void 취소된_예약_시간_예약_성공() {
		//given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(2)
		);

		ReservationRequest.Create request2 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(2)
		);

		String user1Password = "1234";
		ReservationRequest.Delete deleteRequest = new ReservationRequest.Delete(user1Password);

		// when
		reservationService.reserve(request1, user1Id);
		Long reservationId = reservationRepository.findAll()
			.get(0)
			.getId();

		reservationService.cancel(reservationId, deleteRequest, user1Id);

		reservationService.reserve(request2, user2Id);

		Long newReservationId = reservationRepository.findAll()
			.get(0)
			.getId();

		// then
		List<Reservation> reservations = reservationRepository.findAll();
		assertThat(reservations).hasSize(2); // 취소된 예약도 db 삭제가 아니라 상태 전이만 했음

		// RESERVED 상태만 필터링
		Reservation activeReservation = reservations.stream()
			.filter(r -> r.getStatus() == ReservationStatus.RESERVED)
			.findFirst()
			.orElseThrow();

		assertThat(activeReservation)
			.extracting("studyRoom.id", "startAt", "endAt", "status")
			.containsExactly(
				studyRoomId,
				request2.startAt(),
				request2.endAt(),
				ReservationStatus.RESERVED
			);
	}

	@Test
	void 시작시간과_종료시간이_같으면_예약_실패() {
		//given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.of(2026, 1, 10, 10, 0),
			LocalDateTime.of(2026, 1, 10, 10, 0)
		);

		// when, then
		assertThatThrownBy(() -> reservationService.reserve(request, user1Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.INVALID_TIME_RANGE.getMessage());
	}

	@Test
	void 종료시간이_시작시간보다_빠르면_예약_실패() {
		//given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.of(2026, 1, 10, 13, 0),
			LocalDateTime.of(2026, 1, 10, 10, 0)
		);

		// when, then
		assertThatThrownBy(() -> reservationService.reserve(request, user1Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.INVALID_TIME_RANGE.getMessage());
	}

	@Test
	void 과거_시간은_예약할_수_없음() {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().minusHours(1),
			LocalDateTime.now().plusHours(1)
		);
		//when
		assertThatThrownBy(() -> reservationService.reserve(request, user1Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.INVALID_TIME_RANGE.getMessage());
	}

	@Test
	void 미래시간_예약_성공() {
		//given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusMinutes(10),
			LocalDateTime.now().plusHours(1).plusMinutes(10)
		);

		// when
		reservationService.reserve(request, user1Id);

		// then
		assertThat(reservationRepository.findAll()).hasSize(1);
	}

	@Test
	void 사용불가_스터디룸_예약_실패() {
		// given
		StudyRoom disAvailable = new StudyRoom("B룸", false, "테스트룸");
		studyRoomRepository.save(disAvailable);
		Long disAvailableId = disAvailable.getId();

		ReservationRequest.Create request = new ReservationRequest.Create(
			disAvailableId,
			// 테스트에서 날짜 하드 코딩은 위험하다
			// 날짜 하드 코딩하면 시간 제약에 의해 나중에 테스트 돌렸을 때 테스트가 깨진다
			LocalDateTime.now().plusHours(2),
			LocalDateTime.now().plusHours(5)
		);

		// when, then
		assertThatThrownBy(() -> reservationService.reserve(request, user1Id))
			.isInstanceOf(StudyRoomException.class)
			.hasMessage(ExceptionCode.STUDYROOM_NOT_AVAILABLE.getMessage());
	}

	@Test
	void 예약_확인_메서드_성공1() {
		// given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request1, user1Id);

		ReservationRequest.Create request2 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(5),
			LocalDateTime.now().plusHours(6)
		);
		reservationService.reserve(request2, user1Id);

		// when, then
		assertThat(reservationService.reservationConfirm(user1Id)).hasSize(2);
	}

	@Test
	void 예약_확인_메서드_성공2() {
		//when, then
		assertThat(reservationService.reservationConfirm(user1Id)).hasSize(0);
	}

	@Test
	void 예약_수정_성공() {
		// given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request1, user1Id);

		ReservationRequest.Update updateRequest = new ReservationRequest.Update(
			"1234",
			studyRoomId,
			LocalDateTime.now().plusHours(2),
			LocalDateTime.now().plusHours(5)
		);

		Long reservationId = reservationRepository.findAll().get(0).getId();

		// when
		reservationService.update(reservationId, updateRequest, user1Id);

		// then
		Reservation updateReservation = reservationRepository.findById(reservationId).orElse(null);

		assertThat(reservationRepository.findAll()).hasSize(1);

		assertThat(updateReservation)
			.extracting("studyRoom.id", "startAt", "endAt", "status")
			.containsExactly(
				studyRoomId,
				updateRequest.startAt(),
				updateRequest.endAt(),
				ReservationStatus.RESERVED
			);
	}

	@Test
	void 예약_중복으로_예약_수정_실패() {
		// given
		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request1, user1Id);

		Long reservationId = reservationRepository.findAll().get(0).getId();

		ReservationRequest.Create request2 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(5),
			LocalDateTime.now().plusHours(6)
		);
		reservationService.reserve(request2, user1Id);

		ReservationRequest.Update updateRequest = new ReservationRequest.Update(
			"1234",
			studyRoomId,
			LocalDateTime.now().plusHours(3),
			LocalDateTime.now().plusHours(5)
		);

		// when, then
		assertThatThrownBy(() -> reservationService.update(reservationId, updateRequest, user1Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.SCHEDULE_CONFLICT.getMessage());
	}

	@Test
	void 이용_불가_스터디룸_예약_수정_실패() {
		// given
		StudyRoom disAvailableRoom = new StudyRoom("B룸", false, "테스트룸");
		studyRoomRepository.save(disAvailableRoom);
		Long disAvailableRoomId = disAvailableRoom.getId();

		ReservationRequest.Create request1 = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request1, user1Id);
		Long reservationId = reservationRepository.findAll().get(0).getId();

		ReservationRequest.Update updateRequest = new ReservationRequest.Update(
			"1234",
			disAvailableRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);

		// when, then
		assertThatThrownBy(() -> reservationService.update(reservationId, updateRequest, user1Id))
			.isInstanceOf(StudyRoomException.class)
			.hasMessage(ExceptionCode.STUDYROOM_NOT_AVAILABLE.getMessage());
	}

	// IDOR 공격 방어 테스트
	/*
	user1이 예약 생성, reservationId 획득, user2가 해당 reservationId로 수정 시도
	서비스가 예약을 찾지 못하게 차단 -> NOT_FOUND_RESERVATION 발생

	보안 관점에서 공격자가 자신의 계정으로 로그인한 뒤 남의 reservationId를 조작해서 요청하는 공격
	-> 데이터 레벨에서 차단됨
	-> 테스트로 영구 보장됨
	 */
	@Test
	void 타인_예약_수정_공격_실패() {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request, user1Id);
		Long reservationId = reservationRepository
			.findByUserIdAndStatus(user1Id, ReservationStatus.RESERVED)
			.get(0)
			.getId();

		ReservationRequest.Update updateRequest = new ReservationRequest.Update(
			"1234",
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);

		assertThatThrownBy(() -> reservationService.update(reservationId, updateRequest, user2Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.NOT_FOUND_RESERVATION.getMessage());
	}

	@Test
	void 예약_삭제_성공() {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request, user1Id);

		Long reservationId = reservationRepository
			.findByUserIdAndStatus(user1Id, ReservationStatus.RESERVED)
			.get(0)
			.getId();

		ReservationRequest.Delete deleteRequest = new ReservationRequest.Delete("1234");

		// when
		reservationService.cancel(reservationId, deleteRequest, user1Id);

		// then
		Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

		assertThat(reservation.getStatus())
			.isEqualTo(ReservationStatus.CANCELED);
	}

	@Test
	void 타인_예약_삭제_공격_실패() {
		// given
		ReservationRequest.Create request = new ReservationRequest.Create(
			studyRoomId,
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now().plusHours(5)
		);
		reservationService.reserve(request, user1Id);

		Long reservationId = reservationRepository
			.findByUserIdAndStatus(user1Id, ReservationStatus.RESERVED)
			.get(0)
			.getId();

		ReservationRequest.Delete deleteRequest = new ReservationRequest.Delete("1234");

		// when, then
		assertThatThrownBy(() -> reservationService.cancel(reservationId, deleteRequest, user2Id))
			.isInstanceOf(ReservationException.class)
			.hasMessage(ExceptionCode.NOT_FOUND_RESERVATION.getMessage());
	}
}
