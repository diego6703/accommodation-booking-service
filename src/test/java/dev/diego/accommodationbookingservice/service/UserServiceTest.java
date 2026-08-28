package dev.diego.accommodationbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateProfileRequestDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateRoleRequestDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.mapper.UserMapper;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.SecurityService;
import dev.diego.accommodationbookingservice.service.impl.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SecurityService securityService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should return user profile successfully when authenticated user exists")
    void getMyProfile_AuthenticatedUser_ReturnsUserResponseDto() {
        User user = new User();
        UserResponseDto expectedDto = new UserResponseDto(
                1L, "test@test.com", "John", "Doe", Role.CUSTOMER
        );

        when(securityService.getAuthenticatedUser()).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserResponseDto actualDto = userService.getMyProfile();

        assertThat(actualDto).isNotNull();
        assertThat(actualDto).isEqualTo(expectedDto);
        verify(securityService).getAuthenticatedUser();
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Should update user profile successfully without password change")
    void updateMyProfile_ValidRequestWithoutPassword_ReturnsUpdatedDto() {
        User user = new User();
        UserUpdateProfileRequestDto requestDto = new UserUpdateProfileRequestDto(
                "newemail@test.com", null, null, "John", "Doe"
        );
        UserResponseDto expectedDto = new UserResponseDto(
                1L, "newemail@test.com", "John", "Doe", Role.CUSTOMER
        );

        when(securityService.getAuthenticatedUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserResponseDto actualDto = userService.updateMyProfile(requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.email()).isEqualTo("newemail@test.com");
        verify(userMapper).updateUserFromDto(requestDto, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should update user profile successfully with matching passwords")
    void updateMyProfile_ValidRequestWithPassword_ReturnsUpdatedDto() {
        User user = new User();
        UserUpdateProfileRequestDto requestDto = new UserUpdateProfileRequestDto(
                "newemail@test.com", "newPassword123", "newPassword123", "John", "Doe"
        );
        UserResponseDto expectedDto = new UserResponseDto(
                1L, "newemail@test.com", "John", "Doe", Role.CUSTOMER
        );

        when(securityService.getAuthenticatedUser()).thenReturn(user);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserResponseDto actualDto = userService.updateMyProfile(requestDto);

        assertThat(actualDto).isNotNull();
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName(
            "Should throw IllegalArgumentException when passwords do not match in updateMyProfile")
    void updateMyProfile_MismatchedPasswords_ThrowsException() {
        User user = new User();
        UserUpdateProfileRequestDto requestDto = new UserUpdateProfileRequestDto(
                "newemail@test.com", "newPassword123", "differentPassword123", "John", "Doe"
        );

        when(securityService.getAuthenticatedUser()).thenReturn(user);

        assertThatThrownBy(() -> userService.updateMyProfile(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match");
    }

    @Test
    @DisplayName("Should update user role successfully when user exists")
    void updateRole_ExistingId_ReturnsUpdatedUserDto() {
        Long userId = 1L;
        User user = new User();
        UserUpdateRoleRequestDto requestDto = new UserUpdateRoleRequestDto(Role.ADMIN);
        UserResponseDto expectedDto = new UserResponseDto(
                userId, "test@test.com", "John", "Doe", Role.ADMIN
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserResponseDto actualDto = userService.updateRole(userId, requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user ID does not exist in updateRole")
    void updateRole_NonExistingId_ThrowsException() {
        Long userId = 99L;
        UserUpdateRoleRequestDto requestDto = new UserUpdateRoleRequestDto(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(userId, requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find user with id: " + userId);
    }
}
