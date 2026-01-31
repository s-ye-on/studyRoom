package me.studyroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

	@Bean
	public Clock clock() {
		// 운영 환경 기본 시간
		return Clock.systemDefaultZone();
	}
}
