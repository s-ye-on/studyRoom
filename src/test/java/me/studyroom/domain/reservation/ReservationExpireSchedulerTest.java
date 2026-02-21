package me.studyroom.domain.reservation;

import jakarta.transaction.Transactional;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
public class ReservationExpireSchedulerTest {

	// 핵심포인트
	// 1. cron을 실제로 기다리지 않는다 expireWaitPayments() 같은 메서드를 직접 호출
	// 2. Clock을 고정한다 (@MockitoBean Clock)
	// 3. 테스트 데이터는 최소 3종류
	// - 만료 대상 : WAIT_PAYMENT + createdAt이 now-10분보다 과거
	// - 만료 아님 : WAIT_PAYMENT + createdAt이 아직 10분 안지남
	// - 영향 받으면 안 됨 : CONFIRMED 같은 다른 상태
	// 4. 벌크 업데이트면 영속성 컨텍스트가 stale해질 수 있음
	// - 스케줄러 실행 후 Entitymanager.clear() 또는 @Modifying(clearAutomatically = true)로 맞추기 
}
