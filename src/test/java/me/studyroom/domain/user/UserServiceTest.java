package me.studyroom.domain.user;

import jakarta.transaction.Transactional;
import me.studyroom.domain.user.service.UserService;
import me.studyroom.global.dto.request.UserRequest;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.UserException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class UserServiceTest {
	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 유저_가입_성공() {
		// given
		UserRequest.Join request = new UserRequest.Join(
			"최승연",
			"test@naver.com",
			"1234",
			"01011112222");

		// when
		userService.join(request);

		// then
		assertThat(userRepository.findAll().size()).isEqualTo(1);

		assertThat(userRepository.findAll().get(0))
			.extracting(User::getEmail,
				User::getName,
				User::getPhoneNumber,
				User::getPassword)
			.containsExactly(request.email(), request.name(), request.phoneNumber(), request.password());
	}

	@Test
	void 유저_탈퇴_성공() {
		// given
		UserRequest.Join request = new UserRequest.Join(
			"최승연",
			"test@naver.com",
			"1234",
			"01011112222");

		userService.join(request);

		Long userId = userRepository.findAll().get(0).getId();

		UserRequest.Leave leaveRequest = new UserRequest.Leave("test@naver.com", "1234");

		// when
		userService.leave(leaveRequest, userId);

		// then
		assertThat(userRepository.findAll().size()).isEqualTo(0);
	}

	@Test
	void 유저_탈퇴_실패() {
		// given
		UserRequest.Join request = new UserRequest.Join(
			"최승연",
			"test@naver.com",
			"1234",
			"01011112222");

		userService.join(request);

		Long userId = userRepository.findAll().get(0).getId();

		UserRequest.Leave leaveRequest1 = new UserRequest.Leave("test@naver.com", "wrongPassword");
		UserRequest.Leave leaveRequest2 = new UserRequest.Leave("wrongEmail@naver.com", "1234");

		// when, then
		assertThatThrownBy(() -> userService.leave(leaveRequest1, userId))
			.isInstanceOf(UserException.class)
			.hasMessage(ExceptionCode.INVALID_PASSWORD.getMessage());

		assertThatThrownBy(() -> userService.leave(leaveRequest2, userId))
			.isInstanceOf(UserException.class)
			.hasMessage(ExceptionCode.INVALID_EMAIL.getMessage());

	}
}
