package com.booking.service;

import com.booking.entity.User;
import com.booking.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public User findUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User not found, id: " + id));
  }

  public User saveUser(User user) {
    return userRepository.save(user);
  }
}
