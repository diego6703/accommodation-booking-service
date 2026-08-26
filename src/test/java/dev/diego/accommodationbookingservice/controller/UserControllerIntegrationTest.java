package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.user.UserUpdateProfileRequestDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateRoleRequestDto;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class UserControllerIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return logged user")
    void shouldGetMyProfile() {
        User customer = new User();
        customer.setEmail("jack.sparrow@blackpearl.com");
        customer.setPassword(passwordEncoder.encode("rum123"));
        customer.setFirstName("Jack");
        customer.setLastName("Sparrow");
        customer.setRole(Role.CUSTOMER);
        userRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail());

        restTestClient.get()
                .uri("/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.email").isEqualTo("jack.sparrow@blackpearl.com")
                .jsonPath("$.firstName").isEqualTo("Jack")
                .jsonPath("$.lastName").isEqualTo("Sparrow")
                .jsonPath("$.role").isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should let Admin change other user role")
    void shouldUpdateRoleWhenAdmin() {
        String uniqueEmail = "barbossa_" + UUID.randomUUID() + "@blackpearl.com";
        User admin = new User();
        admin.setEmail(uniqueEmail);
        admin.setPassword(passwordEncoder.encode("rum123"));
        admin.setFirstName("Hector");
        admin.setLastName("Barbossa");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String targetEmail = "will_" + UUID.randomUUID() + "@portroyal.com";
        User targetUser = new User();
        targetUser.setEmail(targetEmail);
        targetUser.setPassword(passwordEncoder.encode("sword123"));
        targetUser.setFirstName("Will");
        targetUser.setLastName("Turner");
        targetUser.setRole(Role.CUSTOMER);
        User savedTarget = userRepository.save(targetUser);

        UserUpdateRoleRequestDto requestDto = new UserUpdateRoleRequestDto(Role.ADMIN);
        String token = jwtUtil.generateToken(admin.getEmail());

        restTestClient.put()
                .uri("/users/{id}/role", savedTarget.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should update logged user profile")
    void shouldUpdateMyProfileSuccessfully() {
        String email = "norrington_" + UUID.randomUUID() + "@navy.com";
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("navy123"));
        user.setFirstName("James");
        user.setLastName("Norrington");
        user.setRole(Role.CUSTOMER);
        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail());
        UserUpdateProfileRequestDto requestDto = new UserUpdateProfileRequestDto(
                null, null, null, "Commodore", "Norrington"
        );

        restTestClient.put()
                .uri("/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Commodore")
                .jsonPath("$.lastName").isEqualTo("Norrington");

        User updatedUserFromDb = userRepository.findById(savedUser.getId()).orElseThrow();
        assert updatedUserFromDb.getFirstName().equals("Commodore");
        assert updatedUserFromDb.getLastName().equals("Norrington");
    }
}
