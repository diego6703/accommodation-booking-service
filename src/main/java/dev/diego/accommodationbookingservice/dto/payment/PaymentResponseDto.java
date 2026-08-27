package dev.diego.accommodationbookingservice.dto.payment;

import dev.diego.accommodationbookingservice.model.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponseDto(
        Long id,
        PaymentStatus status,
        Long bookingId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {}
