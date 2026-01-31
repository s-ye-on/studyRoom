package me.studyroom.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.service.UserService;
import me.studyroom.global.dto.request.UserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void join(@Valid @RequestBody UserRequest.Join request) {
		userService.join(request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@Valid @RequestBody UserRequest.Leave request,
										@AuthenticationPrincipal User user) {
		userService.leave(request, user.getId());
	}
}
