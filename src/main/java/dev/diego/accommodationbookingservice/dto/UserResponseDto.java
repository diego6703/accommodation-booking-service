package dev.diego.accommodationbookingservice.dto;

import dev.diego.accommodationbookingservice.model.Role;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {}
