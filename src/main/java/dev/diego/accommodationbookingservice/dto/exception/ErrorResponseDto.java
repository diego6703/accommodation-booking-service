package dev.diego.accommodationbookingservice.dto.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDto(LocalDateTime timestamp, int status, List<String> errors) {}
