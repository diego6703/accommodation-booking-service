package dev.diego.accommodationbookingservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    private User testUser;
    private Accommodation testAccommodation;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAllInBatch();
        accommodationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        testUser = new User();
        testUser.setFirstName("Jack");
        testUser.setLastName("Sparrow");
        testUser.setEmail("jack.sparrow@pearl.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
        userRepository.save(testUser);

        testAccommodation = new Accommodation();
        testAccommodation.setType(AccommodationType.HOUSE);
        testAccommodation.setLocation("Tortuga");
        testAccommodation.setSize("Large");
        testAccommodation.setAmenities(List.of("WiFi"));
        testAccommodation.setDailyRate(BigDecimal.valueOf(150.00));
        testAccommodation.setAvailability(5);
        accommodationRepository.save(testAccommodation);
    }

    @Test
    @DisplayName("Find all bookings by existing user ID should return bookings")
    void findAllByUserId_WithExistingUserId_ShouldReturnBookings() {
        Booking booking = new Booking();
        booking.setUser(testUser);
        booking.setAccommodation(testAccommodation);
        booking.setCheckInDate(LocalDate.of(2026, 9, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 9, 15));
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        List<Booking> actualBookings = bookingRepository.findAllByUserId(testUser.getId());

        assertThat(actualBookings).hasSize(1);
        assertThat(actualBookings.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Find all bookings by non-existing user ID should return empty list")
    void findAllByUserId_WithNonExistingUserId_ShouldReturnEmpty() {
        List<Booking> actualBookings = bookingRepository.findAllByUserId(999L);

        assertThat(actualBookings).isEmpty();
    }

    @Test
    @DisplayName("Find all bookings by status should return matching bookings")
    void findAllByStatus_WithExistingStatus_ShouldReturnBookings() {
        Booking booking = new Booking();
        booking.setUser(testUser);
        booking.setAccommodation(testAccommodation);
        booking.setCheckInDate(LocalDate.of(2026, 9, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 9, 15));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        List<Booking> actualBookings = bookingRepository.findAllByStatus(BookingStatus.CONFIRMED);

        assertThat(actualBookings).hasSize(1);
        assertThat(actualBookings.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Find all bookings by non-existing status should return empty list")
    void findAllByStatus_WithNonExistingStatus_ShouldReturnEmpty() {
        List<Booking> actualBookings = bookingRepository.findAllByStatus(BookingStatus.EXPIRED);

        assertThat(actualBookings).isEmpty();
    }

    @Test
    @DisplayName("Find all bookings by user ID and status should return matching bookings")
    void findAllByUserIdAndStatus_WithValidParams_ShouldReturnBookings() {
        Booking booking = new Booking();
        booking.setUser(testUser);
        booking.setAccommodation(testAccommodation);
        booking.setCheckInDate(LocalDate.of(2026, 9, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 9, 15));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        List<Booking> actualBookings = bookingRepository.findAllByUserIdAndStatus(
                testUser.getId(),
                BookingStatus.CONFIRMED
        );

        assertThat(actualBookings).hasSize(1);
        assertThat(actualBookings.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(actualBookings.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Count overlapping bookings should return correct count ignoring canceled ones")
    void countOverlappingBookings_WithOverlappingDates_ShouldReturnCount() {
        Booking activeBooking = new Booking();
        activeBooking.setUser(testUser);
        activeBooking.setAccommodation(testAccommodation);
        activeBooking.setCheckInDate(LocalDate.of(2026, 9, 10));
        activeBooking.setCheckOutDate(LocalDate.of(2026, 9, 15));
        activeBooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(activeBooking);

        Booking canceledBooking = new Booking();
        canceledBooking.setUser(testUser);
        canceledBooking.setAccommodation(testAccommodation);
        canceledBooking.setCheckInDate(LocalDate.of(2026, 9, 12));
        canceledBooking.setCheckOutDate(LocalDate.of(2026, 9, 18));
        canceledBooking.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(canceledBooking);

        long overlappingCount = bookingRepository.countOverlappingBookings(
                testAccommodation.getId(),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 16)
        );

        assertThat(overlappingCount).isEqualTo(1L);
    }

    @Test
    @DisplayName(
            "Find bookings by status exclusions and checkout date should return matching bookings")
    void findByStatusExclusionsAndDate_ShouldReturnFilteredBookings() {
        final LocalDate targetDate = LocalDate.of(2026, 8, 29);

        Booking matchingBooking = new Booking();
        matchingBooking.setUser(testUser);
        matchingBooking.setAccommodation(testAccommodation);
        matchingBooking.setCheckInDate(LocalDate.of(2026, 8, 20));
        matchingBooking.setCheckOutDate(LocalDate.of(2026, 8, 25));
        matchingBooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(matchingBooking);

        Booking canceledBooking = new Booking();
        canceledBooking.setUser(testUser);
        canceledBooking.setAccommodation(testAccommodation);
        canceledBooking.setCheckInDate(LocalDate.of(2026, 8, 20));
        canceledBooking.setCheckOutDate(LocalDate.of(2026, 8, 25));
        canceledBooking.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(canceledBooking);

        Booking expiredBooking = new Booking();
        expiredBooking.setUser(testUser);
        expiredBooking.setAccommodation(testAccommodation);
        expiredBooking.setCheckInDate(LocalDate.of(2026, 8, 20));
        expiredBooking.setCheckOutDate(LocalDate.of(2026, 8, 25));
        expiredBooking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(expiredBooking);

        Booking futureBooking = new Booking();
        futureBooking.setUser(testUser);
        futureBooking.setAccommodation(testAccommodation);
        futureBooking.setCheckInDate(LocalDate.of(2026, 8, 25));
        futureBooking.setCheckOutDate(LocalDate.of(2026, 9, 2));
        futureBooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(futureBooking);

        List<Booking> actualBookings = bookingRepository
                .findAllByStatusIsNotAndStatusIsNotAndCheckOutDateLessThanEqual(
                        BookingStatus.CANCELED,
                        BookingStatus.EXPIRED,
                        targetDate
                );

        assertThat(actualBookings).hasSize(1);
        assertThat(actualBookings.get(0).getId()).isEqualTo(matchingBooking.getId());
        assertThat(actualBookings.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
