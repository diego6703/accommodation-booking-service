package dev.diego.accommodationbookingservice.controller;

import com.stripe.exception.StripeException;
import dev.diego.accommodationbookingservice.dto.payment.PaymentMessageResponseDto;
import dev.diego.accommodationbookingservice.dto.payment.PaymentResponseDto;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment Controller", description = "Endpoints for managing payments")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create Stripe payment session",
            description = "Creates a new Stripe checkout session for a given booking ID.")
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto createPaymentSession(@RequestParam Long bookingId)
            throws StripeException {
        return paymentService.createCheckoutSession(bookingId);
    }

    @Operation(summary = "Get payments list",
            description = "Retrieves payments for the current user "
                   + "or filters by user ID for admins.")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public List<PaymentResponseDto> getPayments(
            @RequestParam(value = "user_id", required = false) Long userId,
            @AuthenticationPrincipal User currentUser
    ) {
        return paymentService.getPayments(currentUser, userId);
    }

    @Operation(summary = "Handle successful payment",
            description = "Stripe redirect endpoint for successful payments.")
    @GetMapping("/success")
    public PaymentMessageResponseDto paymentSuccess(@RequestParam("session_id") String sessionId) {
        return paymentService.handleSuccessfulPayment(sessionId);
    }

    @Operation(summary = "Handle canceled payment",
            description = "Stripe redirect endpoint for canceled or paused payments.")
    @GetMapping("/cancel")
    public PaymentMessageResponseDto paymentCancel(@RequestParam(
            value = "session_id", required = false) String sessionId) {
        return paymentService.handleCanceledPayment(sessionId);
    }

    @Operation(summary = "Renew expired payment session",
            description = "Creates a new Stripe checkout session for an expired payment.")
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public PaymentResponseDto renewPayment(@PathVariable Long id) {
        return paymentService.renewPayment(id);
    }
}
