package me.studyroom.domain.reservation.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.reservation.Reservation;
import me.studyroom.domain.reservation.ReservationRepository;
import me.studyroom.domain.reservation.ReservationStatus;
import me.studyroom.domain.reservation.dto.ReservationResponse;
import me.studyroom.domain.reservation.policy.ReservationPolicy;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.domain.user.User;
import me.studyroom.global.dto.request.ReservationRequest;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.ReservationException;
import me.studyroom.global.exception.StudyRoomException;
import me.studyroom.global.service.CommonService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final StudyRoomRepository studyRoomRepository;
	private final CommonService commonService;
	private final Clock clock;
	// private final ReservationPolicy reservationPolicy;
	private final List<ReservationPolicy> policies;

	// 지금 만드려고 하는게 현재 시간보다 과거에 시작 시간을 입력값으로 주는걸 막아야함
	// start.isBefore(LocalDateTime.now()) 이렇게 해도 논리적으론 어긋나진 않음
	//

	private void timeValidator(LocalDateTime start, LocalDateTime end) {
		LocalDateTime now = LocalDateTime.now(clock);

		if (!start.isBefore(end)) {
			throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
		}

		if (!start.isAfter(now)) {
			throw new ReservationException(ExceptionCode.INVALID_TIME_RANGE);
		}
	}

	// 예약
	public ReservationResponse.Create reserve(ReservationRequest.Create request, Long userId) {

		User user = commonService.getUserById(userId);

		timeValidator(request.startAt(), request.endAt());

		//StudyRoom studyRoom = commonService.getStudyRoomById(request.studyRoomId());

		// 락 걸고 조회
		StudyRoom studyRoom = studyRoomRepository.findByIdForUpdate(request.studyRoomId())
			.orElseThrow(() -> new StudyRoomException(ExceptionCode.NOT_FOUND_STUDYROOM));

		studyRoom.ensureAvailable();

		// 정책 검증 (운영 시간에 맞게 예약 요청을 했는지)
//		reservationPolicy.validate(
//			request.startAt(),
//			request.endAt(),
//			studyRoom,
//			user
//		);

		// 여러 정책 검증시 사용
		for (ReservationPolicy policy : policies) {
			policy.validate(
				request.startAt(),
				request.endAt(),
				studyRoom,
				user
			);
		}

		boolean existReservation = reservationRepository.existsReservedOverlappingReservation(
			studyRoom,
			ReservationStatus.RESERVED,
			request.startAt(),
			request.endAt()
		);

		if (existReservation) {
			throw new ReservationException(ExceptionCode.SCHEDULE_CONFLICT);
		}

		Reservation reservation = new Reservation(user,
			studyRoom,
			request.startAt(),
			request.endAt());

		reservationRepository.save(reservation);

		return new ReservationResponse.Create(
			user.getName(),
			studyRoom.getName(),
			request.startAt(),
			request.endAt()
		);
	}

	public void confirmPayment() {
	}

	// 예약 확인
	public List<ReservationResponse.Read> reservationConfirm(Long userId) {
		return reservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED)
			.stream()
			.map(this::mapToRead)
			.toList();
	}

	// 변환 책임을 dto 내부에 둬도 되지만 service에 두는게 더 깔끔함 dto는 순수 데이터 구조로 유지
	// DTO가 변환 책임을 갖는 방식은 코드가 짧고 깔끔하고 변환 로직이 DTO에 모여 있긴 함
	// DTO가 도메인(Entity)에 의존함, 계층 의존성이 흐려짐 그래서 나는 변환을 service단에서 하는게 좋다고 생각
	private ReservationResponse.Read mapToRead(Reservation reservation) {
		return new ReservationResponse.Read(
			reservation.getUser().getName(),
			reservation.getStudyRoom().getName(),
			reservation.getStartAt(),
			reservation.getEndAt()
		);
	}

	// 예약 수정
	// 현재 이 업데이트는 방의 가용성에 영향을 끼치는 업데이트이다

	///  todo : 이것도 비관적 lock studyRoom에 걸어서 시간대를 보장하는게 맞아보인다
	/// 해결. 일단 update 하나만 둘 생각이니까 비관적 락을 걸어서 동시성 문제 해결
	/// 예약의 경쟁 자원은 studyRoom이기에 update에도 방 단위 동시성 제어 적용
	/// 예약자 전화번호 변경, 메모 변경 처럼 방 상태와 무관한 것은 그냥 업데이트 해도 좋다 (그래도 낙관적 락은 걸어주는게 좋다)
	/// update를 무엇을 하느냐에 따라 메서드를 분리해보자
	public ReservationResponse.Update update(Long reservationId, ReservationRequest.Update request, Long userId) {
		// 현재 userId와 예약 id를 둘 다 만족해야 예약에 접근가능하게 함으로서 보안사고 방지
		Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
			.orElseThrow(() -> new ReservationException(ExceptionCode.NOT_FOUND_RESERVATION));

		User user = commonService.getUserById(userId);
		user.validatePassword(request.password());


//		reservation.canceled();
		// 이 방법은 추천하지 않는 이유
		// 트랜잭션 롤백 시 상태 꼬일 위험
		// 중간 상태가 존재함
		// 동시성 문제 발생 가능
		// 코드 복잡도 증가

		StudyRoom studyRoom = commonService.getStudyRoomForUpdate(request.StudyRoomId());
		studyRoom.ensureAvailable();

		// 다 좋은데 여기서 걸리는게 있음
		// 만약 방은 그대로하고, 시간도 원래 2-4시 했었는데 이걸 2-3으로 줄이고 싶을수도 있음
		// 근데 여기서 checkExist 보면 원래 예약도 찾아가지고 올텐데 실제로는 예약 가능하지만 가져온 예약을 참조해보면
		// 이미 예약이 되어 있다고 뜨기 때문에 수정이 안될수도 있을 것 같음
		// 해결 방법이 두개로 보임 현재 예약을 cancel 후 다시 예약하기
		// 현재 예약되어 있는 시간도 예약할 수 있는 시간에 추가 시킨 후 조회
		// 근데 서비스는 트랜잭션이라 당장 지금 예약을 캔슬로 보여주고 예약을 바꾸지 않더라도 다시 reserved로 돌아가지 않을까?

		// 기존 예약 상태 canceled로 바꾸고 예약 겹치는지 확인
		// 확인 로직 후 변경 로직 실행 -> 다시 상태 reserved로 변경

		// 이 방법은 추천하지 않음
		// 정석은 "자기 자신 예약 제외"
		boolean checkExist = reservationRepository.existsReservedOverlappingReservationExceptSelf(
			studyRoom,
			ReservationStatus.RESERVED,
			request.startAt(),
			request.endAt(),
			reservationId
		);

		if (checkExist) {
			throw new ReservationException(ExceptionCode.SCHEDULE_CONFLICT);
		}

		reservation.update(studyRoom, request.startAt(), request.endAt());

		reservationRepository.save(reservation);

		return new ReservationResponse.Update(
			user.getName(),
			studyRoom.getName(),
			request.startAt(),
			request.endAt()
		);
	}

	// 예약 삭제
	public void cancel(Long reservationId, ReservationRequest.Delete request, Long userId) {
		User user = commonService.getUserById(userId);
		user.validatePassword(request.password());

		// 여기도 마찬가지로 타인이 프론트에서 아무 예약이나 취소하는 공격을할 수 있음
		// 공격자가 자신의 아이디로 로그인해서 타인의 예약을 취소하기에 아이디와 예약 번호로 찾아서 공격을 막자
//		Reservation reservation = reservationRepository.findById(reservationId)
//			.orElseThrow(() -> new ReservationException(ExceptionCode.NOT_FOUND_RESERVATION));

		Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
			.orElseThrow(() -> new ReservationException(ExceptionCode.NOT_FOUND_RESERVATION));

		reservation.canceled();
	}
}
