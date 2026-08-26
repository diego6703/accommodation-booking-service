package dev.diego.accommodationbookingservice.service;

import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationResponseDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AccommodationService {

    AccommodationResponseDto save(AccommodationRequestDto requestDto);

    List<AccommodationResponseDto> findAll(Pageable pageable);

    AccommodationResponseDto findById(Long id);

    AccommodationResponseDto update(Long id, AccommodationUpdateRequestDto updateDto);

    void deleteById(Long id);
}
