package dev.diego.accommodationbookingservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find user by existing email should return user")
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        String uniqueEmail = "black_" + UUID.randomUUID() + "@pearl.com";
        User user = new User();
        user.setFirstName("Jack");
        user.setLastName("Sparrow");
        user.setEmail(uniqueEmail);
        user.setPassword("encodedPassword");
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        Optional<User> actualUser = userRepository.findByEmail(uniqueEmail);

        assertThat(actualUser).isPresent();
        assertThat(actualUser.get().getEmail()).isEqualTo(uniqueEmail);
        assertThat(actualUser.get().getFirstName()).isEqualTo("Jack");
    }

    @Test
    @DisplayName("Find user by non-existing email should return empty optional")
    void findByEmail_WithNonExistingEmail_ShouldReturnEmpty() {
        Optional<User> actualUser = userRepository.findByEmail("notfound@pearl.com");

        assertThat(actualUser).isEmpty();
    }
}
