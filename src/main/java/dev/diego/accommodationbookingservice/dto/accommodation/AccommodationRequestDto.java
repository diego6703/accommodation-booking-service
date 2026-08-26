package dev.diego.accommodationbookingservice.dto.accommodation;

import dev.diego.accommodationbookingservice.model.AccommodationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record AccommodationRequestDto(
        @NotNull(message = "Accommodation type cannot be null")
        AccommodationType type,

        @NotBlank(message = "Location cannot be blank")
        String location,

        @NotBlank(message = "Size cannot be blank")
        String size,

        List<@NotBlank(message = "Amenity cannot be blank") String> amenities,

        @NotNull(message = "Daily rate cannot be null")
        @DecimalMin(value = "0.01", message = "Daily rate must be greater than 0")
        BigDecimal dailyRate,

        @NotNull(message = "Availability cannot be null")
        @Min(value = 0, message = "Availability cannot be negative")
        Integer availability
) {}
