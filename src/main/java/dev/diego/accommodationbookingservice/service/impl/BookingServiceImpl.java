package dev.diego.accommodationbookingservice.service.impl;

import dev.diego.accommodationbookingservice.dto.booking.BookingRequestDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingUpdateDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.mapper.BookingMapper;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.service.BookingService;
import dev.diego.accommodationbookingservice.service.TelegramNotificationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final BookingMapper bookingMapper;
    private final TelegramNotificationService notificationService;

    @Override
    @Transactional
    public BookingResponseDto createBooking(User currentUser, BookingRequestDto requestDto) {
        validateDates(requestDto.checkInDate(), requestDto.checkOutDate());

        Accommodation accommodation = accommodationRepository.findById(requestDto.accommodationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Accommodation not found with id: " + requestDto.accommodationId()
                ));

        long overlappingCount = bookingRepository.countOverlappingBookings(
                accommodation.getId(),
                requestDto.checkInDate(),
                requestDto.checkOutDate()
        );

        if (overlappingCount >= accommodation.getAvailability()) {
            throw new IllegalStateException(
                    "No available places for this accommodation in the selected date range."
            );
        }

        Booking booking = new Booking();
        booking.setCheckInDate(requestDto.checkInDate());
        booking.setCheckOutDate(requestDto.checkOutDate());
        booking.setAccommodation(accommodation);
        booking.setUser(currentUser);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        notificationService.sendMessage(String.format(
                "🟢 New booking created!\nID: %d\nAccommodation: %s\nFrom: %s\nTo: %s\nUser: %s",
                savedBooking.getId(),
                accommodation.getLocation(), // lub inna nazwa pola z opisem/tytułem accommodation
                savedBooking.getCheckInDate(),
                savedBooking.getCheckOutDate(),
                currentUser.getEmail()
        ));

        return bookingMapper.toDto(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookings(Long userId, BookingStatus status) {
        List<Booking> bookings;

        if (userId != null && status != null) {
            bookings = bookingRepository.findAllByUserIdAndStatus(userId, status);
        } else if (userId != null) {
            bookings = bookingRepository.findAllByUserId(userId);
        } else if (status != null) {
            bookings = bookingRepository.findAllByStatus(status);
        } else {
            bookings = bookingRepository.findAll();
        }

        return bookings.stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(Long userId) {
        return bookingRepository.findAllByUserId(userId).stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long id, User currentUser) {
        Booking booking = findBookingByIdOrThrow(id);
        checkAccess(booking, currentUser);
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDto updateBooking(
            Long id, User currentUser, BookingUpdateDto requestDto) {
        Booking booking = findBookingByIdOrThrow(id);
        checkAccess(booking, currentUser);

        if (requestDto.checkInDate() != null && requestDto.checkOutDate() != null) {
            validateDates(requestDto.checkInDate(), requestDto.checkOutDate());

            long overlappingCount = bookingRepository.countOverlappingBookings(
                    booking.getAccommodation().getId(),
                    requestDto.checkInDate(),
                    requestDto.checkOutDate()
            );

            long currentBookingOverlap = (booking.getCheckInDate().equals(requestDto.checkInDate())
                    && booking.getCheckOutDate().equals(requestDto.checkOutDate())) ? 1 : 0;

            if ((overlappingCount - currentBookingOverlap)
                    >= booking.getAccommodation().getAvailability()) {
                throw new IllegalStateException(
                        "No available places for this accommodation in the new selected date range."
                );
            }

            booking.setCheckInDate(requestDto.checkInDate());
            booking.setCheckOutDate(requestDto.checkOutDate());
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toDto(updatedBooking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long id, User currentUser) {
        Booking booking = findBookingByIdOrThrow(id);
        checkAccess(booking, currentUser);

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new IllegalStateException("Booking is already canceled.");
        }

        booking.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);

        notificationService.sendMessage(String.format(
                "🔴 Booking canceled!\nID: %d\nAccommodation: %s\nFrom: %s\nTo: %s",
                booking.getId(),
                booking.getAccommodation().getLocation(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        ));
    }

    private Booking findBookingByIdOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
    }

    private void checkAccess(Booking booking, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to access this booking.");
        }
    }
}
