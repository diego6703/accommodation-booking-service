package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserLoginResponseDto;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.exception.RegistrationException;
import dev.diego.accommodationbookingservice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication Controller", description =
        "Managing authentication and user registration")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    @Operation(summary = "Register a new user",
            description = "Registers a new user in the system")
    public UserResponseDto register(@RequestBody @Valid UserRegistrationRequestDto request)
            throws RegistrationException {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user",
            description = "Logs in a user and returns a JWT token")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.login(request);
    }
}
