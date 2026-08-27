package dev.diego.accommodationbookingservice.repository;

import dev.diego.accommodationbookingservice.model.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySessionId(String sessionId);

    @Query("SELECT p FROM Payment p JOIN p.booking b JOIN b.user u WHERE u.id = :userId")
    List<Payment> findAllByBookingUserId(@Param("userId") Long userId);
}
