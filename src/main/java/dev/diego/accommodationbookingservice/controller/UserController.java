package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateProfileRequestDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateRoleRequestDto;
import dev.diego.accommodationbookingservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Controller", description = "Endpoints for managing users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile",
            description = "Retrieves the profile information for the currently logged-in user.")
    @GetMapping("/me")
    public UserResponseDto getMyProfile() {
        return userService.getMyProfile();
    }

    @Operation(summary = "Update current user profile",
            description = "Allows users to update their profile information.")
    @PutMapping("/me")
    public UserResponseDto updateMyProfile(
            @RequestBody @Valid UserUpdateProfileRequestDto requestDto) {
        return userService.updateMyProfile(requestDto);
    }

    @Operation(summary = "Update user role",
            description = "Enables users to update their roles, providing role-based access.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public UserResponseDto updateRole(
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateRoleRequestDto requestDto) {
        return userService.updateRole(id, requestDto);
    }
}
