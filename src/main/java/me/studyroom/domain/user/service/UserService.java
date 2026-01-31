package me.studyroom.domain.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.dto.request.UserRequest;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.UserException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	public void join(UserRequest.Join request) {
		User user = new User(
			request.name(),
			request.email(),
			request.password(),
			request.phoneNumber()
		);

		userRepository.save(user);

	}

	// 일단 이렇게 만들어놓고 나중에 soft delete로 전환
	public void leave(UserRequest.Leave request, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		user.validateEmail(request.email());
		user.validatePassword(request.password());

		userRepository.delete(user);
	}
}
