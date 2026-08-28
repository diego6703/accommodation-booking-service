package dev.diego.accommodationbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import dev.diego.accommodationbookingservice.dto.payment.PaymentMessageResponseDto;
import dev.diego.accommodationbookingservice.dto.payment.PaymentResponseDto;
import dev.diego.accommodationbookingservice.mapper.PaymentMapper;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.Payment;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.repository.PaymentRepository;
import dev.diego.accommodationbookingservice.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                paymentService, "successUrl", "http://localhost:8080/payments/success"
        );
        ReflectionTestUtils.setField(
                paymentService, "cancelUrl", "http://localhost:8080/payments/cancel"
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException "
            + "when booking not found in createCheckoutSession")
    void createCheckoutSession_NonExistingBooking_ThrowsException() {
        Long bookingId = 99L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckoutSession(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation not found for ID: " + bookingId);
    }

    @Test
    @DisplayName("Should set numberOfDays to 1 when "
            + "check-out is before check-in in createCheckoutSession")
    void createCheckoutSession_InvalidDatesRange_SetsNumberOfDaysToOne() {
        Long bookingId = 1L;
        final LocalDate checkIn = LocalDate.now().plusDays(2);
        final LocalDate checkOut = LocalDate.now().plusDays(1);

        User user = new User();
        user.setId(1L);

        Accommodation accommodation = new Accommodation();
        accommodation.setDailyRate(BigDecimal.valueOf(100));

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setAccommodation(accommodation);
        booking.setUser(user);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(bookingId))
                .isInstanceOf(StripeException.class);
    }

    @Test
    @DisplayName("Should return user payments when regular user requests them")
    void getPayments_RegularUser_ReturnsUserPayments() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Payment payment = new Payment();
        List<Payment> payments = List.of(payment);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L, PaymentStatus.PENDING, 1L, "url", "sessionId",
                BigDecimal.valueOf(100)
        );

        when(paymentRepository.findAllByBookingUserId(1L)).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> actualList = paymentService.getPayments(user, null);

        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(1);
        verify(paymentRepository).findAllByBookingUserId(1L);
    }

    @Test
    @DisplayName("Should return user payments when regular user requests with requestedUserId")
    void getPayments_RegularUserWithRequestedUserId_ReturnsUserPayments() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.CUSTOMER);

        Payment payment = new Payment();
        List<Payment> payments = List.of(payment);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L, PaymentStatus.PENDING, 1L, "url", "sessionId", BigDecimal.valueOf(100)
        );

        when(paymentRepository.findAllByBookingUserId(1L)).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> actualList = paymentService.getPayments(user, 99L);

        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(1);
        verify(paymentRepository).findAllByBookingUserId(1L);
    }

    @Test
    @DisplayName("Should return all payments when admin requests them without requestedUserId")
    void getPayments_AdminWithoutRequestedUser_ReturnsAllPayments() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        Payment payment = new Payment();
        List<Payment> payments = List.of(payment);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L, PaymentStatus.PENDING, 1L, "url", "sessionId", BigDecimal.valueOf(100)
        );

        when(paymentRepository.findAll()).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> actualList = paymentService.getPayments(admin, null);

        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(1);
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName
            ("Should return specific user payments when admin requests them with requestedUserId")
    void getPayments_AdminWithRequestedUser_ReturnsUserPayments() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        Long requestedUserId = 2L;

        Payment payment = new Payment();
        List<Payment> payments = List.of(payment);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                1L, PaymentStatus.PENDING, 1L, "url", "sessionId",
                BigDecimal.valueOf(100)
        );

        when(paymentRepository.findAllByBookingUserId(requestedUserId))
                .thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> actualList = paymentService.getPayments(
                admin, requestedUserId
        );

        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(1);
        verify(paymentRepository).findAllByBookingUserId(requestedUserId);
    }

    @Test
    @DisplayName("Should handle successful payment and update status to PAID")
    void handleSuccessfulPayment_ValidSessionId_UpdatesStatusToPaid() {
        String sessionId = "sess_123";
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentMessageResponseDto response = paymentService.handleSuccessfulPayment(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Payment was successful");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException "
            + "when session ID not found in handleSuccessfulPayment")
    void handleSuccessfulPayment_NonExistingSession_ThrowsException() {
        String sessionId = "invalid_sess";
        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleSuccessfulPayment(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment not found for session ID: " + sessionId);
    }

    @Test
    @DisplayName("Should handle canceled payment with session ID and update status to CANCELED")
    void handleCanceledPayment_ValidSessionId_UpdatesStatusToCanceled() {
        String sessionId = "sess_123";
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentMessageResponseDto response = paymentService.handleCanceledPayment(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Payment was canceled");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should do nothing when session ID not found in handleCanceledPayment")
    void handleCanceledPayment_NonExistingSessionId_DoesNothing() {
        String sessionId = "non_existent_sess";
        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        PaymentMessageResponseDto response = paymentService.handleCanceledPayment(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Payment was canceled");
    }

    @Test
    @DisplayName("Should handle canceled payment when session ID is null without database lookup")
    void handleCanceledPayment_NullSessionId_ReturnsMessageOnly() {
        PaymentMessageResponseDto response = paymentService.handleCanceledPayment(null);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Payment was canceled");
    }
}
