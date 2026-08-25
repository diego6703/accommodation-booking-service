package dev.diego.accommodationbookingservice.service.impl;

import dev.diego.accommodationbookingservice.dto.auth.UserLoginRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserResponseDto;
import dev.diego.accommodationbookingservice.exception.RegistrationException;
import dev.diego.accommodationbookingservice.mapper.UserMapper;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto request) {
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            throw new RegistrationException("User with this email already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase(),
                        request.password()
                )
        );
        String token = jwtUtil.generateToken(authentication.getName());
        return new UserLoginResponseDto(token);
    }
}
