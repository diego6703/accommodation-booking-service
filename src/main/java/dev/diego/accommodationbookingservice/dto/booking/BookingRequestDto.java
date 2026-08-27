package dev.diego.accommodationbookingservice.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingRequestDto(
        @NotNull(message = "Accommodation ID cannot be null")
        Long accommodationId,

        @NotNull(message = "Check-in date cannot be null")
        @FutureOrPresent(message = "Check-in date must be in the present or future")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date cannot be null")
        @Future(message = "Check-out date must be in the future")
        LocalDate checkOutDate
) {}
