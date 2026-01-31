package me.studyroom.domain.reservation;

import me.studyroom.domain.studyRoom.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
