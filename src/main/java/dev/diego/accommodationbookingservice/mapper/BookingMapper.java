package dev.diego.accommodationbookingservice.mapper;

import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.model.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "accommodationId", source = "accommodation.id")
    @Mapping(target = "userId", source = "user.id")
    BookingResponseDto toDto(Booking booking);
}
