package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.service.TelegramNotificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AccommodationControllerIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TelegramNotificationService telegramNotificationService;

    @BeforeEach
    void setUp() {
        accommodationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should allow unauthenticated users to get all accommodations")
    void shouldGetAllAccommodationsPublicly() {
        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.HOUSE);
        accommodation.setLocation("London, Baker Street");
        accommodation.setSize("Medium");
        accommodation.setAmenities(List.of("WiFi", "Kitchen"));
        accommodation.setDailyRate(BigDecimal.valueOf(150.00));
        accommodation.setAvailability(5);
        accommodationRepository.save(accommodation);

        restTestClient.get()
                .uri("/accommodations")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].location").isEqualTo("London, Baker Street")
                .jsonPath("$[0].dailyRate").isEqualTo(150.00);
    }

    @Test
    @DisplayName("Should let Admin create new accommodation")
    void shouldCreateAccommodationWhenAdmin() {
        String adminEmail = "admin_" + UUID.randomUUID() + "@booking.com";
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Boss");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = jwtUtil.generateToken(admin.getEmail());

        AccommodationRequestDto requestDto = new AccommodationRequestDto(
                AccommodationType.APARTMENT,
                "Warsaw, Marszałkowska",
                "Large",
                List.of("AC", "Parking"),
                BigDecimal.valueOf(250.00),
                3
        );

        restTestClient.post()
                .uri("/accommodations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.location").isEqualTo("Warsaw, Marszałkowska")
                .jsonPath("$.dailyRate").isEqualTo(250.00)
                .jsonPath("$.availability").isEqualTo(3);
    }

    @Test
    @DisplayName("Should forbid Customer from creating accommodation")
    void shouldForbidCustomerWhenCreatingAccommodation() {
        String customerEmail = "customer_" + UUID.randomUUID() + "@booking.com";
        User customer = new User();
        customer.setEmail(customerEmail);
        customer.setPassword(passwordEncoder.encode("customer123"));
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setRole(Role.CUSTOMER);
        userRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail());

        AccommodationRequestDto requestDto = new AccommodationRequestDto(
                AccommodationType.HOUSE,
                "Krakow, Florianska",
                "Small",
                List.of("WiFi"),
                BigDecimal.valueOf(90.00),
                2
        );

        restTestClient.post()
                .uri("/accommodations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should update accommodation details when Admin")
    void shouldUpdateAccommodationWhenAdmin() {
        String adminEmail = "admin_" + UUID.randomUUID() + "@booking.com";
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Boss");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.VILLA);
        accommodation.setLocation("Gdansk, Beachside");
        accommodation.setSize("Extra Large");
        accommodation.setAmenities(List.of("Pool"));
        accommodation.setDailyRate(BigDecimal.valueOf(500.00));
        accommodation.setAvailability(2);
        Accommodation saved = accommodationRepository.save(accommodation);

        AccommodationUpdateRequestDto updateDto = new AccommodationUpdateRequestDto(
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(450.00),
                1
        );
        String token = jwtUtil.generateToken(admin.getEmail());

        restTestClient.put()
                .uri("/accommodations/{id}", saved.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.dailyRate").isEqualTo(450.00)
                .jsonPath("$.availability").isEqualTo(1);
    }

    @Test
    @DisplayName("Should allow unauthenticated users to get accommodation by ID")
    void shouldGetAccommodationByIdPublicly() {
        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.COTTAGE);
        accommodation.setLocation("Poznan, Stary Rynek");
        accommodation.setSize("Small");
        accommodation.setAmenities(List.of("TV"));
        accommodation.setDailyRate(BigDecimal.valueOf(120.00));
        accommodation.setAvailability(3);
        Accommodation saved = accommodationRepository.save(accommodation);

        restTestClient.get()
                .uri("/accommodations/{id}", saved.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.location").isEqualTo("Poznan, Stary Rynek")
                .jsonPath("$.dailyRate").isEqualTo(120.00);
    }

    @Test
    @DisplayName("Should let Admin delete accommodation")
    void shouldDeleteAccommodationWhenAdmin() {
        String adminEmail = "admin_" + UUID.randomUUID() + "@booking.com";
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Boss");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Accommodation accommodation = new Accommodation();
        accommodation.setType(AccommodationType.APARTMENT);
        accommodation.setLocation("Wroclaw, Market Square");
        accommodation.setSize("Medium");
        accommodation.setAmenities(List.of("WiFi", "Balcony"));
        accommodation.setDailyRate(BigDecimal.valueOf(300.00));
        accommodation.setAvailability(1);
        Accommodation saved = accommodationRepository.save(accommodation);

        String token = jwtUtil.generateToken(admin.getEmail());

        restTestClient.delete()
                .uri("/accommodations/{id}", saved.getId())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
        assert accommodationRepository.findById(saved.getId()).isEmpty();
    }
}
