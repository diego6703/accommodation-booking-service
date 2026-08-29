package dev.diego.accommodationbookingservice.scheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(port = 0, baseUrlProperties = "telegram.api.url")
})
class BookingExpirationSchedulerIntegrationTest {

    @Autowired
    private BookingExpirationScheduler scheduler;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Test
    void shouldExpireOldBookingsAndSendTelegramNotification() {
        User user = new User();
        user.setEmail("captain.jack@blackpearl.sea");
        user.setPassword("rumisgonne123");
        user.setFirstName("Jack");
        user.setLastName("Sparrow");
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.VACATION_HOME);
        accommodation.setLocation("Tortuga, Port Royal");
        accommodation.setSize("Medium");
        accommodation.setDailyRate(BigDecimal.valueOf(50));
        accommodation.setAvailability(3);
        accommodation = accommodationRepository.save(accommodation);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().minusDays(5)); // Wymagane przez bazę!
        booking.setCheckOutDate(LocalDate.now().minusDays(1));
        booking.setUser(user);
        booking.setAccommodation(accommodation);

        bookingRepository.save(booking);

        stubFor(post(urlPathMatching("/bot.*?/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        scheduler.checkExpiredBookings();

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.EXPIRED, updatedBooking.getStatus());

        verify(postRequestedFor(urlPathMatching("/bot.*?/sendMessage"))
                .withRequestBody(matchingJsonPath("$.text", matching(".*Booking expired!.*"))));
    }

    @Test
    void shouldNotExpireFutureBookingsOrSendNotification() {
        User user = new User();
        user.setEmail("will.turner@portroyal.sea");
        user.setPassword("blacksmith123");
        user.setFirstName("Will");
        user.setLastName("Turner");
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.HOUSE);
        accommodation.setLocation("Port Royal Forge");
        accommodation.setSize("Small");
        accommodation.setDailyRate(BigDecimal.valueOf(30));
        accommodation.setAvailability(2);
        accommodation = accommodationRepository.save(accommodation);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(5));
        booking.setUser(user);
        booking.setAccommodation(accommodation);

        bookingRepository.save(booking);

        stubFor(post(urlPathMatching("/bot.*?/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        scheduler.checkExpiredBookings();

        Booking unchangedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, unchangedBooking.getStatus());

        verify(postRequestedFor(urlPathMatching("/bot.*?/sendMessage"))
                .withRequestBody(matchingJsonPath(
                        "$.text", matching(".*No expired bookings today!.*"))));
    }
}
