package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationRequestDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationResponseDto;
import dev.diego.accommodationbookingservice.dto.accommodation.AccommodationUpdateRequestDto;
import dev.diego.accommodationbookingservice.service.AccommodationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Accommodation Controller", description = "Endpoints for managing accommodations")
@RestController
@RequestMapping("/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    @Operation(summary = "Create a new accommodation",
            description = "Permits the addition of new accommodations. "
                    + "Accessible only by admin users.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AccommodationResponseDto create(@RequestBody @Valid AccommodationRequestDto requestDto) {
        return accommodationService.save(requestDto);
    }

    @Operation(summary = "Get all accommodations",
            description = "Provides a list of available accommodations "
                    + "with pagination and sorting support. Accessible by all users.")
    @GetMapping
    public List<AccommodationResponseDto> getAll(
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return accommodationService.findAll(pageable);
    }

    @Operation(summary = "Get accommodation by ID",
            description = "Retrieves detailed information "
                    + "about a specific accommodation. Accessible by all users.")
    @GetMapping("/{id}")
    public AccommodationResponseDto getById(@PathVariable Long id) {
        return accommodationService.findById(id);
    }

    @Operation(summary = "Update an accommodation",
            description = "Allows full updates to accommodation details, "
                    + "including inventory management. Accessible only by admin users.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccommodationResponseDto update(
            @PathVariable Long id,
            @RequestBody @Valid AccommodationUpdateRequestDto updateDto
    ) {
        return accommodationService.update(id, updateDto);
    }

    @Operation(summary = "Delete an accommodation",
            description = "DELETE: /accommodations/{id} - Enables the removal of accommodations. "
                    + "Accessible only by admin users.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        accommodationService.deleteById(id);
    }
}
