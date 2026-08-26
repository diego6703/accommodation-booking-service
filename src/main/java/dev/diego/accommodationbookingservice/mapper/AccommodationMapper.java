package dev.diego.accommodationbookingservice.mapper;

import dev.diego.accommodationbookingservice.config.MapperConfig;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationResponseDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import dev.diego.accommodationbookingservice.model.Accommodation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface AccommodationMapper {

    AccommodationResponseDto toDto(Accommodation user);

    Accommodation toEntity(AccommodationRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAccommodationFromDto(AccommodationUpdateRequestDto requestDto,
                                    @MappingTarget Accommodation accommodation);
}
