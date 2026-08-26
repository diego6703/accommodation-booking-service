package dev.diego.accommodationbookingservice.service;

import dev.diego.accommodationbookingservice.dto.UserResponseDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateProfileRequestDto;
import dev.diego.accommodationbookingservice.dto.user.UserUpdateRoleRequestDto;

public interface UserService {

    UserResponseDto getMyProfile();

    UserResponseDto updateMyProfile(UserUpdateProfileRequestDto requestDto);

    UserResponseDto updateRole(Long id, UserUpdateRoleRequestDto requestDto);
}
