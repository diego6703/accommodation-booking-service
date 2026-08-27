package dev.diego.accommodationbookingservice.service;

import dev.diego.accommodationbookingservice.dto.booking.BookingRequestDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingUpdateDto;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.User;
import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(User currentUser, BookingRequestDto requestDto);

    List<BookingResponseDto> getBookings(Long userId, BookingStatus status);

    List<BookingResponseDto> getUserBookings(Long userId);

    BookingResponseDto getBookingById(Long id, User currentUser);

    BookingResponseDto updateBooking(Long id, User currentUser, BookingUpdateDto requestDto);

    void cancelBooking(Long id, User currentUser);
}
