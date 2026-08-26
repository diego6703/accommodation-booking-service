package dev.diego.accommodationbookingservice.dto.accommodation;

import dev.diego.accommodationbookingservice.model.AccommodationType;
import java.math.BigDecimal;
import java.util.List;

public record AccommodationResponseDto(
        Long id,
        AccommodationType type,
        String location,
        String size,
        List<String> amenities,
        BigDecimal dailyRate,
        Integer availability
) {}
