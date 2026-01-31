package me.studyroom.global.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public sealed interface UserRequest permits UserRequest.Join, UserRequest.Leave {

	record Join(
		@NotBlank
		String name,

		@NotBlank
		@Email
		String email,

		@NotBlank
		String password,

		@NotBlank
		String phoneNumber
	) implements UserRequest {
	}

	record Leave(
		@NotBlank
		@Email
		String email,

		@NotBlank
		String password
	) implements UserRequest {
	}
}
