package dev.diego.accommodationbookingservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diego.accommodationbookingservice.dto.payment.PaymentMessageResponseDto;
import dev.diego.accommodationbookingservice.dto.payment.PaymentResponseDto;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PaymentService paymentService;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User customer = new User();
        customer.setEmail("jack.sparrow" + java.util.UUID.randomUUID() + "@blackpearl.com");
        customer.setPassword(passwordEncoder.encode("rum123"));
        customer.setFirstName("Jack");
        customer.setLastName("Sparrow");
        customer.setRole(Role.CUSTOMER);
        User savedUser = userRepository.save(customer);

        token = jwtUtil.generateToken(savedUser.getEmail());
    }

    @Test
    @DisplayName("Should successfully initialize a payment session for an existing booking")
    void shouldCreateCheckoutSession() throws Exception {
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L,
                PaymentStatus.PENDING,
                1L,
                "https://checkout.stripe.com/test-session-url",
                "session_test_id",
                BigDecimal.valueOf(100.00)
        );

        Mockito.when(
                paymentService.createCheckoutSession(Mockito.anyLong())).thenReturn(responseDto);

        mockMvc.perform(post("/payments")
                        .param("bookingId", "1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionUrl")
                        .value("https://checkout.stripe.com/test-session-url"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should retrieve payments list for a given user")
    void shouldGetPaymentsByUser() throws Exception {
        PaymentResponseDto responseDto = new PaymentResponseDto(
                10L,
                PaymentStatus.PENDING,
                1L,
                "https://checkout.stripe.com/test-url",
                "session_test_id",
                BigDecimal.valueOf(250.00)
        );

        Mockito.when(paymentService.getPayments(any(User.class), eq(1L)))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/payments")
                        .param("user_id", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("Should handle successful payment upon return from Stripe")
    void shouldHandlePaymentSuccess() throws Exception {
        String sessionId = "cs_test_a1b2c3d4";
        PaymentMessageResponseDto messageDto =
                new PaymentMessageResponseDto("Payment was successful! The booking has been paid.");

        Mockito.when(paymentService.handleSuccessfulPayment(sessionId)).thenReturn(messageDto);

        mockMvc.perform(get("/payments/success")
                        .param("session_id", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Payment was successful! The booking has been paid."));
    }

    @Test
    @DisplayName("Should handle payment cancellation")
    void shouldHandlePaymentCancel() throws Exception {
        String sessionId = "cs_test_a1b2c3d4";
        PaymentMessageResponseDto messageDto = new PaymentMessageResponseDto(
                "Payment was canceled or put on hold. "
                        + "You can complete the payment later, "
                        + "but please note that the Stripe session is available for only 24 hours."
        );

        Mockito.when(paymentService.handleCanceledPayment(sessionId)).thenReturn(messageDto);

        mockMvc.perform(get("/payments/cancel")
                        .param("session_id", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Payment was canceled or put on hold. "
                                + "You can complete the payment later, "
                                + "but please note that the Stripe session "
                                + "is available for only 24 hours."
                ));
    }

    @Test
    @DisplayName("Should successfully renew expired payment via endpoint")
    void shouldRenewPayment() throws Exception {
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L,
                PaymentStatus.PENDING,
                1L,
                "https://checkout.stripe.com/new-session-url",
                "session_new_id",
                BigDecimal.valueOf(150.00)
        );

        Mockito.when(paymentService.renewPayment(1L)).thenReturn(responseDto);

        mockMvc.perform(post("/payments/1/renew")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionUrl")
                        .value("https://checkout.stripe.com/new-session-url"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
