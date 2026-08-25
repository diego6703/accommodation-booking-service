package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.auth.UserLoginRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserResponseDto;
import dev.diego.accommodationbookingservice.exception.RegistrationException;
import dev.diego.accommodationbookingservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    public UserResponseDto register(@RequestBody @Valid UserRegistrationRequestDto request)
            throws RegistrationException {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.login(request);
    }
}
