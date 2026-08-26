package dev.diego.accommodationbookingservice.service.impl;

import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationResponseDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.mapper.AccommodationMapper;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.service.AccommodationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;

    @Override
    public AccommodationResponseDto save(AccommodationRequestDto requestDto) {
        Accommodation accommodation = accommodationMapper.toEntity(requestDto);
        return accommodationMapper.toDto(accommodationRepository.save(accommodation));
    }

    @Override
    public List<AccommodationResponseDto> findAll(Pageable pageable) {
        return accommodationRepository.findAll(pageable).stream()
                .map(accommodationMapper::toDto)
                .toList();
    }

    @Override
    public AccommodationResponseDto findById(Long id) {
        Accommodation accommodation = accommodationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find accommodation by id: " + id));
        return accommodationMapper.toDto(accommodation);
    }

    @Override
    public AccommodationResponseDto update(Long id, AccommodationUpdateRequestDto updateDto) {
        Accommodation accommodation = accommodationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find accommodation by id: " + id));

        accommodationMapper.updateAccommodationFromDto(updateDto, accommodation);
        return accommodationMapper.toDto(accommodationRepository.save(accommodation));
    }

    @Override
    public void deleteById(Long id) {
        if (!accommodationRepository.existsById(id)) {
            throw new EntityNotFoundException("Can't find accommodation by id: " + id);
        }
        accommodationRepository.deleteById(id);
    }
}
