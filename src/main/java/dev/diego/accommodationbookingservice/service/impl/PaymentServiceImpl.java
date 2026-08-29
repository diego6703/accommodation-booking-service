package dev.diego.accommodationbookingservice.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import dev.diego.accommodationbookingservice.dto.payment.PaymentMessageResponseDto;
import dev.diego.accommodationbookingservice.dto.payment.PaymentResponseDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.exception.PaymentProcessingException;
import dev.diego.accommodationbookingservice.mapper.PaymentMapper;
import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.Payment;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.repository.PaymentRepository;
import dev.diego.accommodationbookingservice.service.PaymentService;
import dev.diego.accommodationbookingservice.service.TelegramNotificationService;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final TelegramNotificationService notificationService;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Override
    @Transactional
    public PaymentResponseDto createCheckoutSession(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reservation not found for ID: " + bookingId));

        long numberOfDays =
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (numberOfDays <= 0) {
            numberOfDays = 1;
        }

        BigDecimal dailyRate = booking.getAccommodation().getDailyRate();
        BigDecimal totalAmount = dailyRate.multiply(BigDecimal.valueOf(numberOfDays));

        String builtSuccessUrl = UriComponentsBuilder.fromUriString(successUrl)
                .queryParam("session_id", "{CHECKOUT_SESSION_ID}")
                .toUriString();

        String builtCancelUrl = UriComponentsBuilder.fromUriString(cancelUrl)
                .queryParam("session_id", "{CHECKOUT_SESSION_ID}")
                .toUriString();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(builtSuccessUrl)
                .setCancelUrl(builtCancelUrl)
                .addLineItem(createLineItem(booking, numberOfDays, totalAmount))
                .putMetadata("bookingId", bookingId.toString())
                .putMetadata("userId", booking.getUser().getId().toString())
                .build();

        Session session;
        try {
            session = Session.create(params);
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create Stripe checkout session", e);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSessionId(session.getId());
        payment.setSessionUrl(session.getUrl());
        payment.setAmountToPay(totalAmount);

        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> getPayments(User currentUser, Long requestedUserId) {
        List<Payment> payments;
        boolean isManager = currentUser.getRole() == Role.ADMIN;

        if (isManager) {
            if (requestedUserId != null) {
                payments = paymentRepository.findAllByBookingUserId(requestedUserId);
            } else {
                payments = paymentRepository.findAll();
            }
        } else {
            payments = paymentRepository.findAllByBookingUserId(currentUser.getId());
        }

        return paymentMapper.toDtoList(payments);
    }

    @Override
    @Transactional
    public PaymentMessageResponseDto handleSuccessfulPayment(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment not found for session ID: " + sessionId));

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        notificationService.sendMessage(String.format(
                "Payment for reservation #%d. Payment was successful! Amount: %s USD",
                payment.getBooking().getId(), payment.getAmountToPay()
        ));

        return new PaymentMessageResponseDto("Payment was successful! The booking has been paid.");
    }

    @Override
    @Transactional
    public PaymentMessageResponseDto handleCanceledPayment(String sessionId) {
        if (sessionId != null) {
            paymentRepository.findBySessionId(sessionId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.CANCELED);
                paymentRepository.save(payment);
            });
        }

        return new PaymentMessageResponseDto(
                "Payment was canceled or put on hold. "
                        + "You can complete the payment later, "
                        + "but please note that the Stripe session is available for only 24 hours."
        );
    }

    private SessionCreateParams.LineItem createLineItem(
            Booking booking, long numberOfDays, BigDecimal totalAmount) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(createPriceData(booking, numberOfDays, totalAmount))
                .build();
    }

    private SessionCreateParams.LineItem.PriceData createPriceData(
            Booking booking, long numberOfDays, BigDecimal totalAmount) {
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        return SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("usd")
                .setUnitAmount(amountInCents)
                .setProductData(createProductData(booking, numberOfDays))
                .build();
    }

    private SessionCreateParams.LineItem.PriceData.ProductData createProductData(
            Booking booking, long numberOfDays) {
        String productName =
                String.format("Booking #%d (%d nights)", booking.getId(), numberOfDays);

        return SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName(productName)
                .build();
    }
}

