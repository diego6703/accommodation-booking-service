package dev.diego.accommodationbookingservice.dto.accommodation;

import dev.diego.accommodationbookingservice.model.AccommodationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

public record AccommodationUpdateRequestDto(
        AccommodationType type,

        String location,

        String size,

        List<String> amenities,

        @DecimalMin(value = "0.01", message = "Daily rate must be greater than 0")
        BigDecimal dailyRate,

        @Min(value = 0, message = "Availability cannot be negative")
        Integer availability
) {}
