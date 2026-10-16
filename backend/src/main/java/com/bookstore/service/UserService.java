package com.bookstore.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.bookstore.dto.RegisterRequest;
import com.bookstore.dto.UpdateProfileRequest;
import com.bookstore.entity.User;
import com.bookstore.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.bookstore.dto.UserResponse;
import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.LoginResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserResponse register(RegisterRequest registerRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        return toResponse(user);
    }

    public UserResponse me(String authorization) {
        return toResponse(currentUser(authorization));
    }

    public UserResponse updateProfile(String authorization, UpdateProfileRequest request) {
        User user = currentUser(authorization);
        user.setName(requireText(request.getName(), "請填寫姓名", 40, "姓名最多 40 個字"));
        user.setPhone(optionalPhone(request.getPhone()));
        user.setAddress(optionalText(request.getAddress(), 200, "地址最多 200 個字"));
        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setName(user.getName());
        userResponse.setPhone(user.getPhone());
        userResponse.setAddress(user.getAddress());
        userResponse.setRole(user.getRole());
        return userResponse;
    }

    private String requireText(String value, String missingMessage, int maxLength, String tooLongMessage) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(missingMessage);
        }
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return text;
    }

    private String optionalText(String value, int maxLength, String tooLongMessage) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return text;
    }

    private String optionalPhone(String value) {
        String phone = value == null ? "" : value.trim();
        if (phone.isEmpty()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (!phone.matches("[0-9+\\-()\\s]+") || digits.length() < 8 || digits.length() > 15) {
            throw new IllegalArgumentException("請填寫有效的電話");
        }
        return phone;
    }

    private User currentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        try {
            String email = jwtService.validateToken(token);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    public LoginResponse login(LoginRequest loginRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect"));
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }
        String token = jwtService.generateToken(user);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRole(user.getRole());
        return response;
    }

}
