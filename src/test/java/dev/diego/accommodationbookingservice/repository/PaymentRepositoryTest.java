package dev.diego.accommodationbookingservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Payment;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    private User testUser;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setFirstName("Jack");
        testUser.setLastName("Sparrow");
        testUser.setEmail("jack.sparrow@pearl.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
        userRepository.save(testUser);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.HOUSE);
        accommodation.setLocation("Tortuga");
        accommodation.setSize("Large");
        accommodation.setAmenities(List.of("WiFi"));
        accommodation.setDailyRate(BigDecimal.valueOf(150.00));
        accommodation.setAvailability(5);
        accommodationRepository.save(accommodation);

        testBooking = new Booking();
        testBooking.setUser(testUser);
        testBooking.setAccommodation(accommodation);
        testBooking.setCheckInDate(LocalDate.of(2026, 9, 10));
        testBooking.setCheckOutDate(LocalDate.of(2026, 9, 15));
        testBooking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(testBooking);
    }

    @Test
    @DisplayName("Find payment by existing session ID should return payment")
    void findBySessionId_WithExistingSessionId_ShouldReturnPayment() {
        String uniqueSessionId = "cs_test_" + UUID.randomUUID();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        payment.setBooking(testBooking);
        payment.setSessionId(uniqueSessionId);
        payment.setSessionUrl("https://checkout.stripe.com/test");
        payment.setAmountToPay(BigDecimal.valueOf(750.00));
        payment.setExpiresAt(LocalDateTime.now().plusHours(24));
        paymentRepository.save(payment);

        Optional<Payment> actualPayment = paymentRepository.findBySessionId(uniqueSessionId);

        assertThat(actualPayment).isPresent();
        assertThat(actualPayment.get().getSessionId()).isEqualTo(uniqueSessionId);
        assertThat(actualPayment.get().getAmountToPay())
                .isEqualByComparingTo(BigDecimal.valueOf(750.00));
    }

    @Test
    @DisplayName("Find payment by non-existing session ID should return empty optional")
    void findBySessionId_WithNonExistingSessionId_ShouldReturnEmpty() {
        Optional<Payment> actualPayment = paymentRepository.findBySessionId("non_existent_session");

        assertThat(actualPayment).isEmpty();
    }

    @Test
    @DisplayName("Find all payments by booking user ID should return matching payments")
    void findAllByBookingUserId_WithExistingUserId_ShouldReturnPayments() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PAID);
        payment.setBooking(testBooking);
        payment.setSessionId("cs_test_" + UUID.randomUUID());
        payment.setSessionUrl("https://checkout.stripe.com/test");
        payment.setAmountToPay(BigDecimal.valueOf(750.00));
        payment.setExpiresAt(LocalDateTime.now().plusHours(24));
        paymentRepository.save(payment);

        List<Payment> actualPayments = paymentRepository.findAllByBookingUserId(testUser.getId());

        assertThat(actualPayments).hasSize(1);
        assertThat(actualPayments.get(0).getBooking().getUser().getId())
                .isEqualTo(testUser.getId());
        assertThat(actualPayments.get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Find all payments by non-existing user ID should return empty list")
    void findAllByBookingUserId_WithNonExistingUserId_ShouldReturnEmpty() {
        List<Payment> actualPayments = paymentRepository.findAllByBookingUserId(999L);

        assertThat(actualPayments).isEmpty();
    }

    @Test
    @DisplayName("Find by status and expiresAt before should return expired pending payments")
    void findByStatusAndExpiresAtBefore_ShouldReturnExpiredPayments() {
        Payment expiredPayment = new Payment();
        expiredPayment.setStatus(PaymentStatus.PENDING);
        expiredPayment.setBooking(testBooking);
        expiredPayment.setSessionId("cs_test_expired_" + UUID.randomUUID());
        expiredPayment.setSessionUrl("https://checkout.stripe.com/test");
        expiredPayment.setAmountToPay(BigDecimal.valueOf(100.00));
        expiredPayment.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        paymentRepository.save(expiredPayment);

        Booking secondBooking = new Booking();
        secondBooking.setUser(testUser);
        secondBooking.setAccommodation(testBooking.getAccommodation());
        secondBooking.setCheckInDate(LocalDate.of(2026, 10, 1));
        secondBooking.setCheckOutDate(LocalDate.of(2026, 10, 5));
        secondBooking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(secondBooking);

        Payment activePayment = new Payment();
        activePayment.setStatus(PaymentStatus.PENDING);
        activePayment.setBooking(secondBooking);
        activePayment.setSessionId("cs_test_active_" + UUID.randomUUID());
        activePayment.setSessionUrl("https://checkout.stripe.com/test");
        activePayment.setAmountToPay(BigDecimal.valueOf(200.00));
        activePayment.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        paymentRepository.save(activePayment);

        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore(
                PaymentStatus.PENDING, LocalDateTime.now()
        );

        assertThat(expiredPayments).hasSize(1);
        assertThat(expiredPayments.get(0).getSessionId()).isEqualTo(expiredPayment.getSessionId());
    }
}
