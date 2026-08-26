package dev.diego.accommodationbookingservice.dto.user;

import dev.diego.accommodationbookingservice.model.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRoleRequestDto(
        @NotNull(message = "Role cannot be null")
        Role role
) {}
