package me.studyroom.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.studyroom.global.exception.ExceptionCode;
import me.studyroom.global.exception.UserException;

@Entity
@Table(name = "users") // 절대 테이블 이름을 user로 쓰지말자 USER는 SQL 표준 예약어, MySQL 예약어, H2 예약어이다
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(nullable = false, length = 50, unique = true)
	private String email;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, length = 11, unique = true)
	private String phoneNumber;

	public User(String name, String email, String password, String phoneNumber) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.phoneNumber = phoneNumber;
	}

	public void validatePassword(String password) {
		if (!password.equals(this.password)) {
			throw new UserException(ExceptionCode.INVALID_PASSWORD);
		}
	}

	public void validateEmail(String email) {
		if (!email.equals(this.email)) {
			throw new UserException(ExceptionCode.INVALID_EMAIL);
		}
	}
}
