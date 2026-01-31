package me.studyroom.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.UserException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	// username email 사용 (로그인 ID를 말하는 것)
	@Override
	@NonNull // 이거랑 매개 변수에 붙어있는 것 없어도 됨 그냥 상위 인터페이스 메서드에 null 관련 어노테이션이 있는데
	// 내가 오버라이드한 메서드에는 없기 때문에 노란줄이 떴을 뿐
	public UserDetails loadUserByUsername(@NonNull String username) {
		User user = userRepository.findByEmail(username)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		return new CustomUserDetails(user);
	}
}
