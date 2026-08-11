package com.taskapp.backend.service;

import com.taskapp.backend.dto.AuthRequest;
import com.taskapp.backend.dto.AuthResponse;
import com.taskapp.backend.dto.UserResponse;
import com.taskapp.backend.exception.AuthenticationException;
import com.taskapp.backend.exception.UserConflictException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse register(AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserConflictException(username);
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(normalizeDisplayName(request.getDisplayName(), username));
        user.setPasswordSalt("");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthToken(generateToken());
        user.setOnboardingStatus("PENDING");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        AppUser saved = userRepository.save(user);
        return toAuthResponse(saved, saved.getAuthToken());
    }

    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordMatches(user, request.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }

        String token = generateToken();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if (isLegacyPasswordHash(user.getPasswordHash())) {
            String upgradedHash = passwordEncoder.encode(request.getPassword());
            userRepository.updatePassword(user.getId(), upgradedHash, "", now);
            user.setPasswordHash(upgradedHash);
            user.setPasswordSalt("");
        }
        userRepository.updateToken(user.getId(), token, now);
        user.setAuthToken(token);
        return toAuthResponse(user, token);
    }

    public AppUser requireUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            throw new AuthenticationException("Authentication required");
        }
        return userRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("Authentication required"));
    }

    public UserResponse me(String authorizationHeader) {
        return toUserResponse(requireUser(authorizationHeader));
    }

    public void logout(String authorizationHeader) {
        AppUser user = requireUser(authorizationHeader);
        userRepository.clearToken(user.getId(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public UserResponse toUserResponse(AppUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setOnboardingStatus(user.getOnboardingStatus());
        return response;
    }

    private AuthResponse toAuthResponse(AppUser user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(toUserResponse(user));
        return response;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return fallback;
        }
        return displayName.trim();
    }

    private String generateToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean passwordMatches(AppUser user, String password) {
        if (!isLegacyPasswordHash(user.getPasswordHash())) {
            return passwordEncoder.matches(password, user.getPasswordHash());
        }
        return legacyPasswordHash(user.getPasswordSalt(), password).equals(user.getPasswordHash());
    }

    private boolean isLegacyPasswordHash(String passwordHash) {
        return passwordHash == null || !passwordHash.startsWith("$2");
    }

    private String legacyPasswordHash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
