package dev.diego.accommodationbookingservice.dto.auth;

import dev.diego.accommodationbookingservice.annotation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FieldMatch(
        field = "password",
        fieldMatch = "repeatPassword",
        message = "Passwords must match"
)
public record UserRegistrationRequestDto(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
        String password,

        @NotBlank(message = "Repeat password cannot be blank")
        @Size(min = 8, max = 20, message = "Repeat password must be between 8 and 20 characters")
        String repeatPassword,

        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName
) {
}
