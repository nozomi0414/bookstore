package com.bookstore.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.bookstore.service.UserService;
import com.bookstore.dto.RegisterRequest;
import com.bookstore.dto.UserResponse;
import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.LoginResponse;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/register")
	public UserResponse register(@RequestBody RegisterRequest registerRequest) {
        return userService.register(registerRequest);
	}
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest);
    }
}
