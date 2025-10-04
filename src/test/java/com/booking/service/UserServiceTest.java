package com.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.entity.User;
import com.booking.mapper.UserMapper;
import com.booking.repository.UserRepository;
import com.booking.web.dto.user.UserCreateRequest;
import com.booking.web.dto.user.UserResponse;
import com.booking.web.dto.user.UserUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserService userService;

  private User mockUser;
  private UserResponse mockResponse;

  @BeforeEach
  void setUp() {
    mockUser = User.builder()
        .id(1L)
        .name("John Doe")
        .build();

    mockResponse = new UserResponse(
        1L,
        "John Doe"
    );
  }

  @Test
  @DisplayName("Should create user successfully")
  void create_ShouldCreateUser() {
    // Given
    UserCreateRequest request = new UserCreateRequest("John Doe");

    when(userMapper.toEntity(request)).thenReturn(mockUser);
    when(userRepository.save(mockUser)).thenReturn(mockUser);
    when(userMapper.toDto(mockUser)).thenReturn(mockResponse);

    // When
    UserResponse result = userService.create(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("John Doe");

    verify(userMapper).toEntity(request);
    verify(userRepository).save(mockUser);
    verify(userMapper).toDto(mockUser);
  }

  @Test
  @DisplayName("Should get user by id successfully")
  void getById_WhenUserExists_ShouldReturnUser() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
    when(userMapper.toDto(mockUser)).thenReturn(mockResponse);

    // When
    UserResponse result = userService.getById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("John Doe");

    verify(userRepository).findById(1L);
    verify(userMapper).toDto(mockUser);
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void getById_WhenUserNotExists_ShouldThrowException() {
    // Given
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.getById(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User 999 not found");

    verify(userRepository).findById(999L);
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("Should update user successfully")
  void update_WhenUserExists_ShouldUpdateUser() {
    // Given
    UserUpdateRequest request = new UserUpdateRequest("Jane Smith");

    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
    doNothing().when(userMapper).update(mockUser, request);
    when(userMapper.toDto(mockUser)).thenReturn(mockResponse);

    // When
    UserResponse result = userService.update(1L, request);

    // Then
    assertThat(result).isNotNull();

    verify(userRepository).findById(1L);
    verify(userMapper).update(mockUser, request);
    verify(userMapper).toDto(mockUser);
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent user")
  void update_WhenUserNotExists_ShouldThrowException() {
    // Given
    UserUpdateRequest request = new UserUpdateRequest("John Doe");

    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.update(999L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User 999 not found");

    verify(userRepository).findById(999L);
    verify(userMapper, never()).update(any(), any());
  }

  @Test
  @DisplayName("Should delete user successfully")
  void delete_WhenUserExists_ShouldDeleteUser() {
    // Given
    when(userRepository.existsById(1L)).thenReturn(true);
    doNothing().when(userRepository).deleteById(1L);

    // When
    userService.delete(1L);

    // Then
    verify(userRepository).existsById(1L);
    verify(userRepository).deleteById(1L);
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent user")
  void delete_WhenUserNotExists_ShouldThrowException() {
    // Given
    when(userRepository.existsById(999L)).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> userService.delete(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User 999 not found");

    verify(userRepository).existsById(999L);
    verify(userRepository, never()).deleteById(any());
  }
}