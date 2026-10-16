package com.bookstore.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.bookstore.entity.User;
import com.bookstore.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

	private final UserRepository userRepository;

	@Value("${app.admin.email:}")
	private String email;

	@Value("${app.admin.password:}")
	private String password;

	@Value("${app.admin.name:管理員}")
	private String name;

	@Override
	public void run(ApplicationArguments args) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}
		if (userRepository.findByEmail(email).isPresent()) {
			return;
		}
		User admin = new User();
		admin.setEmail(email);
		admin.setPassword(new BCryptPasswordEncoder().encode(password));
		admin.setName(name);
		admin.setRole("ADMIN");
		admin.setCreatedAt(LocalDateTime.now());
		admin.setUpdatedAt(LocalDateTime.now());
		userRepository.save(admin);
	}
}
