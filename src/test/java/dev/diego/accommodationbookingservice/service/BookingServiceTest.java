package dev.diego.accommodationbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diego.accommodationbookingservice.dto.booking.BookingRequestDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingUpdateDto;
import dev.diego.accommodationbookingservice.exception.BookingException;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.exception.OverlappingBookingException;
import dev.diego.accommodationbookingservice.mapper.BookingMapper;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.service.impl.BookingServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private TelegramNotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName("Should create booking successfully and send notification")
    void createBooking_ValidRequest_ReturnsBookingResponseDto() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setLocation("Warsaw");
        accommodation.setAvailability(5);

        Booking savedBooking = new Booking();
        BookingResponseDto expectedDto = new BookingResponseDto(
                1L, checkIn, checkOut, 1L, 1L, BookingStatus.PENDING
        );

        when(accommodationRepository.findById(1L)).thenReturn(Optional.of(accommodation));
        when(bookingRepository.countOverlappingBookings(1L, checkIn, checkOut)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toDto(savedBooking)).thenReturn(expectedDto);

        BookingRequestDto requestDto = new BookingRequestDto(1L, checkIn, checkOut);
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");

        BookingResponseDto actualDto = bookingService.createBooking(user, requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.id()).isEqualTo(1L);
        verify(notificationService).sendMessage(any());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when check-out date is before check-in")
    void createBooking_InvalidDates_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        BookingRequestDto requestDto = new BookingRequestDto(1L, checkIn, checkOut);
        User user = new User();

        assertThatThrownBy(() -> bookingService.createBooking(user, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Check-out date must be after check-in date.");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no available places exist")
    void createBooking_NoAvailability_ThrowsException() {
        final LocalDate checkIn = LocalDate.now().plusDays(1);
        final LocalDate checkOut = LocalDate.now().plusDays(3);

        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setAvailability(1);

        when(accommodationRepository.findById(1L)).thenReturn(Optional.of(accommodation));
        when(bookingRepository.countOverlappingBookings(1L, checkIn, checkOut))
                .thenReturn(1L);

        BookingRequestDto requestDto = new BookingRequestDto(1L, checkIn, checkOut);
        User user = new User();

        assertThatThrownBy(() -> bookingService.createBooking(user, requestDto))
                .isInstanceOf(OverlappingBookingException.class)
                .hasMessage(
                        "No available places for this accommodation in the selected date range.");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when accommodation not found")
    void createBooking_AccommodationNotFound_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        BookingRequestDto requestDto = new BookingRequestDto(99L, checkIn, checkOut);
        User user = new User();

        assertThatThrownBy(() -> bookingService.createBooking(user, requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Accommodation not found with id: 99");
    }

    @Test
    @DisplayName("Should return bookings filtered by userId and status")
    void getBookings_WithUserIdAndStatus_ReturnsFilteredBookings() {
        Long userId = 1L;
        BookingStatus status = BookingStatus.PENDING;
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findAllByUserIdAndStatus(userId, status)).thenReturn(bookings);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(
                new BookingResponseDto(1L,
                        LocalDate.now(), LocalDate.now().plusDays(1), 1L, 1L, status)
        );

        List<BookingResponseDto> result = bookingService.getBookings(userId, status);

        assertThat(result).hasSize(1);
        verify(bookingRepository).findAllByUserIdAndStatus(userId, status);
    }

    @Test
    @DisplayName("Should return bookings filtered by userId only")
    void getBookings_WithUserIdOnly_ReturnsUserBookings() {
        Long userId = 1L;
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findAllByUserId(userId)).thenReturn(bookings);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(
                new BookingResponseDto(1L, LocalDate.now(), LocalDate.now().plusDays(1),
                        1L, 1L, BookingStatus.PENDING)
        );

        List<BookingResponseDto> result = bookingService.getBookings(userId, null);

        assertThat(result).hasSize(1);
        verify(bookingRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("Should return bookings filtered by status only")
    void getBookings_WithStatusOnly_ReturnsStatusBookings() {
        BookingStatus status = BookingStatus.PENDING;
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findAllByStatus(status)).thenReturn(bookings);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(
                new BookingResponseDto(1L, LocalDate.now(),
                        LocalDate.now().plusDays(1), 1L, 1L, status)
        );

        List<BookingResponseDto> result = bookingService.getBookings(null, status);

        assertThat(result).hasSize(1);
        verify(bookingRepository).findAllByStatus(status);
    }

    @Test
    @DisplayName("Should return all bookings when no filters provided")
    void getBookings_NoFilters_ReturnsAllBookings() {
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findAll()).thenReturn(bookings);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(
                new BookingResponseDto(1L, LocalDate.now(),
                        LocalDate.now().plusDays(1), 1L, 1L, BookingStatus.PENDING)
        );

        List<BookingResponseDto> result = bookingService.getBookings(null, null);

        assertThat(result).hasSize(1);
        verify(bookingRepository).findAll();
    }

    @Test
    @DisplayName("Should return user bookings list directly")
    void getUserBookings_ValidUser_ReturnsBookings() {
        Long userId = 1L;
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findAllByUserId(userId)).thenReturn(bookings);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(
                new BookingResponseDto(1L, LocalDate.now(),
                        LocalDate.now().plusDays(1), 1L, 1L, BookingStatus.PENDING)
        );

        List<BookingResponseDto> result = bookingService.getUserBookings(userId);

        assertThat(result).hasSize(1);
        verify(bookingRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("Should return booking when owner requests it")
    void getBookingById_Owner_ReturnsBookingResponseDto() {
        final Long bookingId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Accommodation accommodation = new Accommodation();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setAccommodation(accommodation);

        BookingResponseDto expectedDto = new BookingResponseDto(
                bookingId, LocalDate.now(),
                LocalDate.now().plusDays(2), 1L, 1L, BookingStatus.PENDING
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

        BookingResponseDto actualDto = bookingService.getBookingById(bookingId, user);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.id()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("Should return booking when admin requests it")
    void getBookingById_Admin_ReturnsBookingResponseDto() {
        final Long bookingId = 1L;
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);

        User owner = new User();
        owner.setId(1L);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(owner);

        BookingResponseDto expectedDto = new BookingResponseDto(
                bookingId, LocalDate.now(),
                LocalDate.now().plusDays(2), 1L, 1L, BookingStatus.PENDING
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

        BookingResponseDto actualDto = bookingService.getBookingById(bookingId, admin);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.id()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner non-admin requests booking")
    void getBookingById_UnauthorizedUser_ThrowsException() {
        final Long bookingId = 1L;
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.CUSTOMER);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setRole(Role.CUSTOMER);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(owner);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(bookingId, otherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this booking.");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when booking not found by ID")
    void getBookingById_NotFound_ThrowsException() {
        Long bookingId = 99L;
        User user = new User();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(bookingId, user))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found with id: 99");
    }

    @Test
    @DisplayName("Should update booking successfully when new dates are valid and available")
    void updateBooking_ValidRequest_UpdatesBooking() {
        final LocalDate newCheckIn = LocalDate.now().plusDays(5);
        final LocalDate newCheckOut = LocalDate.now().plusDays(7);

        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setAvailability(2);

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));

        BookingResponseDto expectedDto = new BookingResponseDto(
                bookingId, newCheckIn, newCheckOut, 1L, 1L, BookingStatus.PENDING
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.countOverlappingBookings(1L, newCheckIn, newCheckOut))
                .thenReturn(0L);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

        BookingUpdateDto requestDto = new BookingUpdateDto(newCheckIn, newCheckOut);
        BookingResponseDto actualDto = bookingService.updateBooking(bookingId, user, requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.checkInDate()).isEqualTo(newCheckIn);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should update booking successfully when dates remain the same")
    void updateBooking_SameDates_UpdatesSuccessfully() {
        final LocalDate checkIn = LocalDate.now().plusDays(1);
        final LocalDate checkOut = LocalDate.now().plusDays(3);

        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setAvailability(1);

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);

        BookingResponseDto expectedDto = new BookingResponseDto(
                bookingId, checkIn, checkOut, 1L, 1L, BookingStatus.PENDING
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.countOverlappingBookings(1L, checkIn, checkOut))
                .thenReturn(1L);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

        BookingUpdateDto requestDto = new BookingUpdateDto(checkIn, checkOut);
        BookingResponseDto actualDto = bookingService.updateBooking(bookingId, user, requestDto);

        assertThat(actualDto).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no availability during update")
    void updateBooking_NoAvailability_ThrowsException() {
        final LocalDate newCheckIn = LocalDate.now().plusDays(5);
        final LocalDate newCheckOut = LocalDate.now().plusDays(7);

        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setAvailability(1);

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.countOverlappingBookings(1L, newCheckIn, newCheckOut))
                .thenReturn(1L);

        BookingUpdateDto requestDto = new BookingUpdateDto(newCheckIn, newCheckOut);

        assertThatThrownBy(() -> bookingService.updateBooking(bookingId, user, requestDto))
                .isInstanceOf(OverlappingBookingException.class)
                .hasMessage("No available places for "
                        + "this accommodation in the new selected date range.");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when update dates are invalid")
    void updateBooking_InvalidDates_ThrowsException() {
        final LocalDate checkIn = LocalDate.now().plusDays(5);
        final LocalDate checkOut = LocalDate.now().plusDays(3);

        User user = new User();
        user.setId(1L);

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingUpdateDto requestDto = new BookingUpdateDto(checkIn, checkOut);

        assertThatThrownBy(() -> bookingService.updateBooking(bookingId, user, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Check-out date must be after check-in date.");
    }

    // --- cancelBooking ---

    @Test
    @DisplayName("Should cancel booking successfully and send notification")
    void cancelBooking_ValidOwner_CancelsBooking() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Accommodation accommodation = new Accommodation();
        accommodation.setLocation("Warsaw");

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setStatus(BookingStatus.PENDING);
        booking.setAccommodation(accommodation);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        bookingService.cancelBooking(bookingId, user);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        verify(notificationService).sendMessage(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when booking is already canceled")
    void cancelBooking_AlreadyCanceled_ThrowsException() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        final Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUser(user);
        booking.setStatus(BookingStatus.CANCELED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, user))
                .isInstanceOf(BookingException.class)
                .hasMessage("Booking is already canceled.");
    }
}
