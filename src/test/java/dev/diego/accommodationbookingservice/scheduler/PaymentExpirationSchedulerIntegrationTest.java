package dev.diego.accommodationbookingservice.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Payment;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.repository.PaymentRepository;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(port = 0, baseUrlProperties = "telegram.api.url")
})
class PaymentExpirationSchedulerIntegrationTest {

    @Autowired
    private PaymentExpirationScheduler scheduler;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Test
    void shouldExpireOldPendingPayments() {
        User user = new User();
        user.setEmail("elizabeth.swann@portroyal.sea");
        user.setPassword("pirateking123");
        user.setFirstName("Elizabeth");
        user.setLastName("Swann");
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.VACATION_HOME);
        accommodation.setLocation("Shipwreck Cove");
        accommodation.setSize("Large");
        accommodation.setDailyRate(BigDecimal.valueOf(100));
        accommodation.setAvailability(2);
        accommodation = accommodationRepository.save(accommodation);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(10));
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        payment.setBooking(booking);
        payment.setSessionId("cs_test_expired_session");
        payment.setSessionUrl("https://checkout.stripe.com/test");
        payment.setAmountToPay(BigDecimal.valueOf(500));
        payment.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        paymentRepository.save(payment);

        scheduler.checkExpiredPayments();

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.EXPIRED, updatedPayment.getStatus());
    }

    @Test
    void shouldNotExpireValidPayments() {
        User user = new User();
        user.setEmail("hector.barbossa@blackpearl.sea");
        user.setPassword("apples123");
        user.setFirstName("Hector");
        user.setLastName("Barbossa");
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.HOUSE);
        accommodation.setLocation("Isla de Muerta");
        accommodation.setSize("Medium");
        accommodation.setDailyRate(BigDecimal.valueOf(80));
        accommodation.setAvailability(1);
        accommodation = accommodationRepository.save(accommodation);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setCheckInDate(LocalDate.now().plusDays(2));
        booking.setCheckOutDate(LocalDate.now().plusDays(7));
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        payment.setBooking(booking);
        payment.setSessionId("cs_test_valid_session");
        payment.setSessionUrl("https://checkout.stripe.com/test");
        payment.setAmountToPay(BigDecimal.valueOf(400));
        payment.setExpiresAt(LocalDateTime.now().plusHours(2));
        paymentRepository.save(payment);

        scheduler.checkExpiredPayments();

        Payment unchangedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, unchangedPayment.getStatus());
    }
}
