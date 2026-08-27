package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.booking.BookingRequestDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingUpdateDto;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Booking management",
        description = "endpoints for creating,viewing,updating and canceling bookings")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new booking",
            description = "Allows authenticated users to create a new accommodation booking.")
    public BookingResponseDto createBooking(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid BookingRequestDto requestDto
    ) {
        return bookingService.createBooking(currentUser, requestDto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (Manager)",
            description = "Retrieves bookings based on optional user ID and status. "
                    + "Available for managers/admins.")
    public List<BookingResponseDto> getBookings(
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false) BookingStatus status
    ) {
        return bookingService.getBookings(userId, status);
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user bookings",
            description = "Retrieves bookings belonging to the currently authenticated user.")
    public List<BookingResponseDto> getMyBookings(
            @AuthenticationPrincipal User currentUser
    ) {
        return bookingService.getUserBookings(currentUser.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID",
            description = "Provides detailed information about a specific booking.")
    public BookingResponseDto getBookingById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return bookingService.getBookingById(id, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update booking details",
            description = "Allows users to update their booking details (e.g., dates).")
    public BookingResponseDto updateBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody @Valid BookingUpdateDto requestDto
    ) {
        return bookingService.updateBooking(id, currentUser, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a booking",
            description = "Enables the cancellation of a booking (prevents double cancellation).")
    public void cancelBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        bookingService.cancelBooking(id, currentUser);
    }
}
