package me.studyroom.domain.studyRoom.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.studyroom.domain.studyRoom.StudyRoom;
import me.studyroom.domain.studyRoom.StudyRoomRepository;
import me.studyroom.global.dto.request.StudyRoomRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class StudyRoomService {
	private final StudyRoomRepository studyRoomRepository;

	public void create(StudyRoomRequest.Create request) {
		StudyRoom studyRoom = new StudyRoom(
			request.name(),
			request.available(),
			request.description(),
			request.openTime(),
			request.closeTime()
		);
		studyRoomRepository.save(studyRoom);
	}

	// 여기서 지금 헷갈리는게
	// update할 때 모든 부분을 업데이트 안할수도 있음 예를 들어 이용 가능 여부만 업데이트하거나, 설명만 업데이트할 수 있는데
	// dto에서 @NotBlank로 막아버리면 입력을 똑같은 것도 받아야하므로 사용자 경험이 저하될 수 있을거라 생각함
	// 그렇다면 updateDescription, updateAvailable로 나누어야하나 고민
	public void update() {}
}
