package dev.diego.accommodationbookingservice.scheduler;

import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.repository.BookingRepository;
import dev.diego.accommodationbookingservice.service.TelegramNotificationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Warsaw") //
    @Transactional
    public void checkExpiredBookings() {
        LocalDate thresholdDate = LocalDate.now().plusDays(1);

        List<Booking> expiredBookings = bookingRepository
                .findAllByStatusIsNotAndStatusIsNotAndCheckOutDateLessThanEqual(
                        BookingStatus.CANCELED,
                        BookingStatus.EXPIRED,
                        thresholdDate
                );

        if (expiredBookings.isEmpty()) {
            telegramNotificationService.sendMessage("No expired bookings today!");
            return;
        }

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            String message = String.format(
                    "Booking expired!\nID: %d\nAccommodation ID: %d\nUser ID: %d\nCheck-out: %s",
                    booking.getId(),
                    booking.getAccommodation().getId(),
                    booking.getUser().getId(),
                    booking.getCheckOutDate()
            );
            telegramNotificationService.sendMessage(message);
        }
    }
}
