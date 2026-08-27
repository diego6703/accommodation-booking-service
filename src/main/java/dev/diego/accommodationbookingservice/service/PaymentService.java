package dev.diego.accommodationbookingservice.service;

import com.stripe.exception.StripeException;
import dev.diego.accommodationbookingservice.dto.payment.PaymentMessageResponseDto;
import dev.diego.accommodationbookingservice.dto.payment.PaymentResponseDto;
import dev.diego.accommodationbookingservice.model.User;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto createCheckoutSession(Long bookingId) throws StripeException;

    List<PaymentResponseDto> getPayments(User currentUser, Long requestedUserId);

    PaymentMessageResponseDto handleSuccessfulPayment(String sessionId);

    PaymentMessageResponseDto handleCanceledPayment(String sessionId);
}

