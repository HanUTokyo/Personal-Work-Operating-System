package com.taskapp.backend.service;

import com.taskapp.backend.dto.AuthRequest;
import com.taskapp.backend.dto.AuthResponse;
import com.taskapp.backend.exception.AuthenticationException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    @Test
    void registerStoresBcryptPasswordWithoutLegacySalt() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        AuthResponse response = authService.register(request(" Alice ", "correct horse", "Alice"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser saved = userCaptor.getValue();
        assertEquals("alice", saved.getUsername());
        assertEquals("", saved.getPasswordSalt());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches("correct horse", saved.getPasswordHash()));
        assertEquals(42L, response.getUser().getId());
        assertFalse(response.getToken().isBlank());
    }

    @Test
    void loginAcceptsBcryptWithoutRewritingPassword() {
        AppUser user = user("alice", new BCryptPasswordEncoder(4).encode("secret"), "");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request("alice", "secret", null));

        assertFalse(response.getToken().isBlank());
        verify(userRepository, never()).updatePassword(anyLong(), anyString(), anyString(), any(LocalDateTime.class));
        verify(userRepository).updateToken(anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void loginUpgradesLegacySha256PasswordAfterSuccessfulAuthentication() throws Exception {
        String salt = "legacy-salt";
        AppUser user = user("legacy", legacyHash(salt, "secret"), salt);
        when(userRepository.findByUsername("legacy")).thenReturn(Optional.of(user));

        authService.login(request("legacy", "secret", null));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).updatePassword(anyLong(), hashCaptor.capture(), org.mockito.ArgumentMatchers.eq(""), any(LocalDateTime.class));
        assertTrue(hashCaptor.getValue().startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches("secret", hashCaptor.getValue()));
    }

    @Test
    void loginRejectsWrongPasswordWithoutChangingCredentials() {
        AppUser user = user("alice", new BCryptPasswordEncoder(4).encode("secret"), "");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () -> authService.login(request("alice", "wrong", null)));

        verify(userRepository, never()).updatePassword(anyLong(), anyString(), anyString(), any(LocalDateTime.class));
        verify(userRepository, never()).updateToken(anyLong(), anyString(), any(LocalDateTime.class));
    }

    private AuthRequest request(String username, String password, String displayName) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setDisplayName(displayName);
        return request;
    }

    private AppUser user(String username, String hash, String salt) {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setPasswordHash(hash);
        user.setPasswordSalt(salt);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private String legacyHash(String salt, String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }
}
