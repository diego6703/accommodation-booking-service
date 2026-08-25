package dev.diego.accommodationbookingservice.service;

import dev.diego.accommodationbookingservice.dto.auth.UserLoginRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserResponseDto;

public interface AuthenticationService {

    UserResponseDto register(UserRegistrationRequestDto request);

    UserLoginResponseDto login(UserLoginRequestDto request);
}
