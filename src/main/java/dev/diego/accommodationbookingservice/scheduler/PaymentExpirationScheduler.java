package dev.diego.accommodationbookingservice.scheduler;

import dev.diego.accommodationbookingservice.model.Payment;
import dev.diego.accommodationbookingservice.model.PaymentStatus;
import dev.diego.accommodationbookingservice.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationScheduler {

    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 * * * * *") // co minutę
    @Transactional
    public void checkExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepository
                .findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, now);

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} expired payments.", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
        }

        paymentRepository.saveAll(expiredPayments);
    }
}
