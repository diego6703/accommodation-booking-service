package dev.diego.accommodationbookingservice.repository;

import dev.diego.accommodationbookingservice.model.Booking;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByUserId(Long userId);

    List<Booking> findAllByStatus(BookingStatus status);

    List<Booking> findAllByUserIdAndStatus(Long userId, BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.accommodation.id = :accommodationId "
            + "AND b.status NOT IN ('CANCELED', 'EXPIRED') "
            + "AND b.checkInDate < :checkOutDate "
            + "AND b.checkOutDate > :checkInDate")
    long countOverlappingBookings(
            @Param("accommodationId") Long accommodationId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    List<Booking> findAllByStatusIsNotAndStatusIsNotAndCheckOutDateLessThanEqual(
            BookingStatus status1,
            BookingStatus status2,
            LocalDate date
    );
}
