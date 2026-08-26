package dev.diego.accommodationbookingservice.dto.user;

import dev.diego.accommodationbookingservice.annotation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@FieldMatch(
        field = "password",
        fieldMatch = "repeatPassword",
        message = "Passwords must match"
)
public record UserUpdateProfileRequestDto(
        @Email(message = "Invalid email format")
        String email,

        @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
        String password,

        @Size(min = 8, max = 20, message = "Repeat password must be between 8 and 20 characters")
        String repeatPassword,

        String firstName,
        String lastName
) {}
