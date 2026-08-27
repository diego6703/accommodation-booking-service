package dev.diego.accommodationbookingservice.repository;

import dev.diego.accommodationbookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
