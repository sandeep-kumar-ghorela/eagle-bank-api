package com.eaglebank.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.eaglebank.entity.UserEntity;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.impl.AuthServiceImpl;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String EXPIRED_TOKEN = "Token is expired"; 
    private final static String EMAIL = "test@example.com";
    private final static String PASSWORD = "password";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
        user.setRole("ROLE_USER");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = authService.loadUserByUsername(EMAIL);

        // Assert
        assertNotNull(userDetails);
        assertEquals(EMAIL, userDetails.getUsername());
        assertEquals(PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                           .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsSignatureException() {
        // Arrange
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        io.jsonwebtoken.security.SignatureException thrown = assertThrows(SignatureException.class, () -> {
            authService.loadUserByUsername(EMAIL);
        });

        assertEquals(EXPIRED_TOKEN, thrown.getMessage());
    }
}
