package com.restaurant.service;

import com.restaurant.dto.auth.AuthResponse;
import com.restaurant.dto.auth.LoginRequest;
import com.restaurant.model.User;
import com.restaurant.model.enums.Role;
import com.restaurant.repository.UserRepository;
import com.restaurant.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Jane Staff")
                .username("janestaff")
                .email("jane@example.com")
                .password("encodedPassword")
                .role(Role.STAFF)
                .tokenVersion(0L)
                .build();
    }

    @Test
    void login_success_bumpsTokenVersionAndReturnsToken() {
        LoginRequest request = new LoginRequest("janestaff", "Password123");

        when(userRepository.findByUsername("janestaff")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("janestaff");
        assertThat(response.role()).isEqualTo("STAFF");
        // tokenVersion must be incremented before the token is issued
        assertThat(sampleUser.getTokenVersion()).isEqualTo(1L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(sampleUser);
    }

    @Test
    void login_badCredentials_throws() {
        LoginRequest request = new LoginRequest("janestaff", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownUsername_throws() {
        LoginRequest request = new LoginRequest("nobody", "Password123");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
