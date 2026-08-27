package dev.diego.accommodationbookingservice.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;

public record BookingUpdateDto(
        @FutureOrPresent(message = "Check-in date must be in the present or future")
        LocalDate checkInDate,

        @Future(message = "Check-out date must be in the future")
        LocalDate checkOutDate
) {}
