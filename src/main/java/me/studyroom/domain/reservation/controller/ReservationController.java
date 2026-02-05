package me.studyroom.domain.reservation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.reservation.dto.ReservationResponse;
import me.studyroom.domain.reservation.service.ReservationService;
import me.studyroom.global.dto.request.ReservationRequest;
import me.studyroom.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
	private final ReservationService reservationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse.Create reserve(@Valid @RequestBody ReservationRequest.Create createRequest,
																						@AuthenticationPrincipal CustomUserDetails user) {
		return reservationService.reserve(createRequest, user.getId());
	}

	@PostMapping("/{id}/confirm")
	public void confirm(@PathVariable Long id) {

	}


	@PutMapping("/{reservationId}")
	public ReservationResponse.Update updateReservation(@PathVariable Long reservationId,
																											@Valid @RequestBody ReservationRequest.Update updateRequest,
																											@AuthenticationPrincipal CustomUserDetails user) {
		return reservationService.update(reservationId, updateRequest, user.getId());
	}

	@GetMapping
	public List<ReservationResponse.Read> getReservations(@AuthenticationPrincipal CustomUserDetails user) {
		return reservationService.reservationConfirm(user.getId());
	}

	///  todo : 비밀번호 검증 제거(이미 인증됨), delete는 path + 인증 정보만 사용
	@DeleteMapping("/{reservationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelReservation(@PathVariable Long reservationId,
																@Valid @RequestBody ReservationRequest.Delete deleteRequest,
																@AuthenticationPrincipal CustomUserDetails user) {
		reservationService.cancel(reservationId, deleteRequest, user.getId());
	}
}
