package dev.diego.accommodationbookingservice.mapper;

import dev.diego.accommodationbookingservice.config.MapperConfig;
import dev.diego.accommodationbookingservice.dto.auth.UserRegistrationRequestDto;
import dev.diego.accommodationbookingservice.dto.auth.UserResponseDto;
import dev.diego.accommodationbookingservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface UserMapper {

    UserResponseDto toDto(User user);

    @Mapping(target = "email", expression = "java(requestDto.email().toLowerCase())")
    User toEntity(UserRegistrationRequestDto requestDto);
}
