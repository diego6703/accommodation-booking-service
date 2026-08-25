package dev.diego.accommodationbookingservice.dto.auth;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName
) {}
