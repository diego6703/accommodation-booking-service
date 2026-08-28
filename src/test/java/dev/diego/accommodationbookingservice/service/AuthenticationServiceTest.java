package dev.diego.accommodationbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.exception.RegistrationException;
import dev.diego.accommodationbookingservice.mapper.UserMapper;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.service.impl.AuthenticationServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("Should register user successfully when email is unique")
    void register_UniqueEmail_ReturnsUserResponseDto() {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "test@test.com", "password123", "password123", "John", "Doe"
        );
        User user = new User();
        User savedUser = new User();
        UserResponseDto expectedDto = new UserResponseDto(
                1L, "test@test.com", "John", "Doe", Role.CUSTOMER
        );

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userMapper.toEntity(requestDto)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedDto);

        UserResponseDto actualDto = authenticationService.register(requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.email()).isEqualTo("test@test.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw RegistrationException when email already exists in register")
    void register_ExistingEmail_ThrowsException() {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "test@test.com", "password123", "password123", "John", "Doe"
        );
        User existingUser = new User();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authenticationService.register(requestDto))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("User with this email already exists");
    }

    @Test
    @DisplayName("Should login user successfully and return JWT token")
    void login_ValidCredentials_ReturnsJwtToken() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto("test@test.com", "password123");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@test.com");
        when(jwtUtil.generateToken("test@test.com")).thenReturn("mocked.jwt.token");

        UserLoginResponseDto responseDto = authenticationService.login(requestDto);

        assertThat(responseDto).isNotNull();
        assertThat(responseDto.token()).isEqualTo("mocked.jwt.token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("test@test.com");
    }
}
