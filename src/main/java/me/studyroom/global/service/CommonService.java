package me.studyroom.global.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.domain.user.User;
import me.studyroom.domain.user.UserRepository;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.StudyRoomException;
import me.studyroom.global.exception.UserException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class CommonService {
	private final UserRepository userRepository;
	private final StudyRoomRepository studyRoomRepository;

	// 엔티티 가져오기 전략 캡슐화
	// 락 정책 공통 서비스에 위임

	// User 관련
	public User getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));
	}

	// StudyRoom 관련
	// studyRoom 단순 조회
	public StudyRoom getStudyRoomById(Long studyRoomId) {
		return studyRoomRepository.findById(studyRoomId)
			.orElseThrow(() -> new StudyRoomException(ExceptionCode.NOT_FOUND_STUDYROOM));
	}

	// studyRoom 락 걸고 조회
	public StudyRoom getStudyRoomForUpdate(Long studyRoomId) {
		return studyRoomRepository.findByIdForUpdate(studyRoomId)
			.orElseThrow(() -> new StudyRoomException(ExceptionCode.NOT_FOUND_STUDYROOM));
	}

}
