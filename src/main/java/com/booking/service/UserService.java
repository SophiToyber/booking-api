package com.booking.service;

import com.booking.entity.User;
import com.booking.mapper.UserMapper;
import com.booking.repository.UserRepository;
import com.booking.web.dto.user.UserCreateRequest;
import com.booking.web.dto.user.UserResponse;
import com.booking.web.dto.user.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public UserResponse create(UserCreateRequest req) {
    User entity = userMapper.toEntity(req);
    User saved = userRepository.save(entity);
    return userMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public UserResponse getById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User %d not found".formatted(id)));
    return userMapper.toDto(user);
  }

  public UserResponse update(Long id, UserUpdateRequest req) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User %d not found".formatted(id)));
    userMapper.update(user, req);
    return userMapper.toDto(user);
  }

  public void delete(Long id) {
    try {
      userRepository.deleteById(id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found".formatted(id));
    }
  }
}
