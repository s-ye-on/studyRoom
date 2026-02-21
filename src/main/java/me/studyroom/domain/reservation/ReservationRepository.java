package me.studyroom.domain.reservation;

import me.studyroom.domain.studyRoom.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	@Query("""
		SELECT count(r)>0
		 FROM Reservation r
		  WHERE r.studyRoom = :studyRoom
		   AND r.startAt < :endAt
		    AND r.endAt > :startAt
		     AND r.status = :status
		""")
	boolean existsReservedOverlappingReservation(
		@Param("studyRoom") StudyRoom studyRoom,
		@Param("status") ReservationStatus status,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt
	);

	// <> : SQL/ JPQL에서 "같지 않다(NOT EQUAL)" 연산자

	@Query("""
		SELECT count (r) > 0
		FROM Reservation r
		WHERE r.studyRoom = :studyRoom
		AND r.status = :status
		AND r.startAt < :endAt
		AND r.endAt > :startAt
		AND r.id <> :reservationId
		""")
	boolean existsReservedOverlappingReservationExceptSelf(
		@Param("studyRoom") StudyRoom studyRoom,
		@Param("status") ReservationStatus status,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt,
		@Param("reservationId") Long reservationId
	);

	List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

	Optional<Reservation> findByIdAndUserId(Long reservationId, Long userId);

	// clearAutomatically=true : 벌크 업데이트 후 1차 캐시 비워서 상태 꼬임 방지
	// flushAutomatically=true : 혹시 같은 트랜잭션에서 쌓인 변경사항이 있으면 먼저 flush
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE Reservation r
		SET r.status = :expired
		WHERE r.status = :wait
		AND r.createdAt < :deadline
""")
	int expireWaitPayments(
		@Param("wait") ReservationStatus wait,
		@Param("expired") ReservationStatus expired,
		@Param("deadline") LocalDateTime deadline
	);
}
