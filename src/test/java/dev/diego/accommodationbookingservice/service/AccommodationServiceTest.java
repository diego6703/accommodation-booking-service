package dev.diego.accommodationbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationResponseDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import dev.diego.accommodationbookingservice.exception.EntityNotFoundException;
import dev.diego.accommodationbookingservice.mapper.AccommodationMapper;
import dev.diego.accommodationbookingservice.model.Accommodation;
import dev.diego.accommodationbookingservice.model.AccommodationType;
import dev.diego.accommodationbookingservice.repository.AccommodationRepository;
import dev.diego.accommodationbookingservice.service.impl.AccommodationServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private AccommodationMapper accommodationMapper;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @InjectMocks
    private AccommodationServiceImpl accommodationService;

    @Test
    @DisplayName("Should save an accommodation successfully and send notification")
    void save_ValidRequest_ReturnsAccommodationResponseDto() {
        final AccommodationRequestDto requestDto = new AccommodationRequestDto(
                AccommodationType.HOUSE, "Warsaw", "Large",
                List.of("WiFi"), BigDecimal.valueOf(150.00), 5
        );
        Accommodation accommodation = new Accommodation();
        accommodation.setId(1L);
        accommodation.setType(AccommodationType.HOUSE);
        accommodation.setLocation("Warsaw");
        accommodation.setDailyRate(BigDecimal.valueOf(150.00));

        AccommodationResponseDto expectedDto = new AccommodationResponseDto(
                1L, AccommodationType.HOUSE, "Warsaw", "Large",
                List.of("WiFi"), BigDecimal.valueOf(150.00), 5
        );

        when(accommodationMapper.toEntity(requestDto)).thenReturn(accommodation);
        when(accommodationRepository.save(accommodation)).thenReturn(accommodation);
        when(accommodationMapper.toDto(accommodation)).thenReturn(expectedDto);

        AccommodationResponseDto actualDto = accommodationService.save(requestDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.location()).isEqualTo("Warsaw");
        verify(accommodationRepository).save(accommodation);
        verify(telegramNotificationService).sendMessage(
                "🏠 New accommodation created!\nID: 1\nType: "
                        + "HOUSE\nLocation: Warsaw\nDaily Rate: $150.0"
        );
    }

    @Test
    @DisplayName("Should return list of accommodations when findAll is called")
    void findAll_ValidPageable_ReturnsListOfAccommodations() {
        Pageable pageable = PageRequest.of(0, 10);
        Accommodation accommodation = new Accommodation();
        AccommodationResponseDto responseDto = new AccommodationResponseDto(
                1L, AccommodationType.HOUSE, "Warsaw", "Large",
                List.of("WiFi"), BigDecimal.valueOf(150.00), 5
        );
        Page<Accommodation> accommodationPage = new PageImpl<>(List.of(accommodation), pageable, 1);

        when(accommodationRepository.findAll(pageable)).thenReturn(accommodationPage);
        when(accommodationMapper.toDto(accommodation)).thenReturn(responseDto);

        List<AccommodationResponseDto> actualList = accommodationService.findAll(pageable);

        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(1);
        assertThat(actualList.get(0).location()).isEqualTo("Warsaw");
    }

    @Test
    @DisplayName("Should return accommodation by ID when accommodation exists")
    void findById_ExistingId_ReturnsAccommodationResponseDto() {
        Long accommodationId = 1L;
        Accommodation accommodation = new Accommodation();
        AccommodationResponseDto expectedDto = new AccommodationResponseDto(
                accommodationId, AccommodationType.HOUSE, "Warsaw", "Large",
                List.of("WiFi"), BigDecimal.valueOf(150.00), 5
        );

        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));
        when(accommodationMapper.toDto(accommodation)).thenReturn(expectedDto);

        AccommodationResponseDto actualDto = accommodationService.findById(accommodationId);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.id()).isEqualTo(accommodationId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when "
            + "accommodation ID does not exist in findById")
    void findById_NonExistingId_ThrowsException() {
        Long accommodationId = 99L;
        when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accommodationService.findById(accommodationId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation by id: " + accommodationId);
    }

    @Test
    @DisplayName("Should update accommodation successfully when ID exists")
    void update_ExistingId_ReturnsUpdatedAccommodationResponseDto() {
        Long accommodationId = 1L;
        AccommodationUpdateRequestDto updateDto = new AccommodationUpdateRequestDto(
                AccommodationType.APARTMENT, "Krakow", "Medium",
                List.of("AC"), BigDecimal.valueOf(200.00), 3
        );
        Accommodation accommodation = new Accommodation();
        AccommodationResponseDto expectedDto = new AccommodationResponseDto(
                accommodationId, AccommodationType.APARTMENT, "Krakow", "Medium",
                List.of("AC"), BigDecimal.valueOf(200.00), 3
        );

        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));
        when(accommodationRepository.save(accommodation)).thenReturn(accommodation);
        when(accommodationMapper.toDto(accommodation)).thenReturn(expectedDto);

        AccommodationResponseDto actualDto =
                accommodationService.update(accommodationId, updateDto);

        assertThat(actualDto).isNotNull();
        assertThat(actualDto.location()).isEqualTo("Krakow");
        verify(accommodationMapper).updateAccommodationFromDto(updateDto, accommodation);
        verify(accommodationRepository).save(accommodation);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existing accommodation")
    void update_NonExistingId_ThrowsException() {
        Long accommodationId = 99L;
        AccommodationUpdateRequestDto updateDto = new AccommodationUpdateRequestDto(
                null, null, null, null, null, null
        );
        when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accommodationService.update(accommodationId, updateDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation by id: " + accommodationId);
    }

    @Test
    @DisplayName("Should delete accommodation successfully when ID exists and send notification")
    void deleteById_ExistingId_DeletesAccommodation() {
        Long accommodationId = 1L;
        Accommodation accommodation = new Accommodation();
        accommodation.setId(accommodationId);
        accommodation.setLocation("Warsaw");

        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));

        accommodationService.deleteById(accommodationId);

        verify(accommodationRepository).deleteById(accommodationId);
        verify(telegramNotificationService).sendMessage(
                "🗑️ Accommodation deleted/released!\nID: 1\nLocation: Warsaw"
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existing accommodation")
    void deleteById_NonExistingId_ThrowsException() {
        Long accommodationId = 99L;
        when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accommodationService.deleteById(accommodationId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation by id: " + accommodationId);

        verify(accommodationRepository, never()).deleteById(any());
        verify(telegramNotificationService, never()).sendMessage(any());
    }
}
