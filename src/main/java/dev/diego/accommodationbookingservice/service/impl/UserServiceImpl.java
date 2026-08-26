package dev.diego.accommodationbookingservice.service.impl;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateProfileRequestDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateRoleRequestDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.mapper.UserMapper;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.SecurityService;
import dev.diego.accommodationbookingservice.service.UserService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile() {
        User user = securityService.getAuthenticatedUser();
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateMyProfile(UserUpdateProfileRequestDto requestDto) {
        User user = securityService.getAuthenticatedUser();

        if (requestDto.password() != null || requestDto.repeatPassword() != null) {
            if (!Objects.equals(requestDto.password(), requestDto.repeatPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }
        }
        userMapper.updateUserFromDto(requestDto, user);
        if (requestDto.password() != null && !requestDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(requestDto.password()));
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateRole(Long id, UserUpdateRoleRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user with id: " + id));
        user.setRole(requestDto.role());
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }
}
