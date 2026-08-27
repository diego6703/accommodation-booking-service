package dev.diego.accommodationbookingservice.dto.booking;

import dev.diego.accommodationbookingservice.model.BookingStatus;
import java.time.LocalDate;

public record BookingResponseDto(
        Long id,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long accommodationId,
        Long userId,
        BookingStatus status
) {}
