package com.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Should save and find user by id")
  void save_ShouldPersistUser() {
    // Given
    User user = User.builder()
        .name("John Doe")
        .build();

    // When
    User saved = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // Then
    Optional<User> found = userRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("John Doe");
    assertThat(found.get().getId()).isNotNull();
  }

  @Test
  @DisplayName("Should return empty when user not found")
  void findById_WhenNotExists_ShouldReturnEmpty() {
    // When
    Optional<User> found = userRepository.findById(999L);

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find all users")
  void findAll_ShouldReturnAllUsers() {
    // Given - create multiple users
    User user1 = createUser("Alice");
    User user2 = createUser("Bob");
    User user3 = createUser("Charlie");
    entityManager.flush();

    // When
    List<User> users = userRepository.findAll();

    // Then
    assertThat(users).hasSize(3);
    assertThat(users)
        .extracting(User::getName)
        .containsExactlyInAnyOrder("Alice", "Bob", "Charlie");
  }

  @Test
  @DisplayName("Should update user")
  void update_ShouldModifyUser() {
    // Given
    User user = User.builder()
        .name("Original Name")
        .build();
    user = entityManager.persistAndFlush(user);
    entityManager.clear();

    // When
    User found = userRepository.findById(user.getId()).orElseThrow();
    found.setName("Updated Name");
    userRepository.save(found);
    entityManager.flush();
    entityManager.clear();

    // Then
    User updated = userRepository.findById(user.getId()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("Updated Name");
  }

  @Test
  @DisplayName("Should delete user")
  void delete_ShouldRemoveUser() {
    // Given
    User user = User.builder()
        .name("To Delete")
        .build();
    user = entityManager.persistAndFlush(user);
    Long userId = user.getId();
    entityManager.clear();

    // When
    userRepository.deleteById(userId);
    entityManager.flush();

    // Then
    Optional<User> found = userRepository.findById(userId);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should check if user exists by id")
  void existsById_ShouldReturnCorrectValue() {
    // Given
    User user = createUser("Test User");
    entityManager.flush();

    // When & Then
    assertThat(userRepository.existsById(user.getId())).isTrue();
    assertThat(userRepository.existsById(999L)).isFalse();
  }

  @Test
  @DisplayName("Should count users")
  void count_ShouldReturnCorrectCount() {
    // Given
    createUser("User 1");
    createUser("User 2");
    createUser("User 3");
    entityManager.flush();

    // When
    long count = userRepository.count();

    // Then
    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("Should delete all users")
  void deleteAll_ShouldRemoveAllUsers() {
    // Given
    createUser("User 1");
    createUser("User 2");
    createUser("User 3");
    entityManager.flush();

    // When
    userRepository.deleteAll();
    entityManager.flush();

    // Then
    assertThat(userRepository.count()).isZero();
  }

  @Test
  @DisplayName("Should save user with long name")
  void save_LongName_ShouldWork() {
    // Given - name with 255 characters (database limit)
    String longName = "A".repeat(255);
    User user = User.builder()
        .name(longName)
        .build();

    // When
    User saved = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // Then
    User found = userRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).hasSize(255);
    assertThat(found.getName()).isEqualTo(longName);
  }

  @Test
  @DisplayName("Should save user with special characters in name")
  void save_SpecialCharacters_ShouldWork() {
    // Given
    String nameWithSpecialChars = "Jean-François O'Brien";
    User user = User.builder()
        .name(nameWithSpecialChars)
        .build();

    // When
    User saved = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // Then
    User found = userRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo(nameWithSpecialChars);
  }

  @Test
  @DisplayName("Should handle multiple saves of same user")
  void save_MultipleTimes_ShouldUpdateNotCreate() {
    // Given
    User user = createUser("Original");
    entityManager.flush();
    Long userId = user.getId();

    // When - update same user
    user.setName("Updated 1");
    userRepository.save(user);
    entityManager.flush();

    user.setName("Updated 2");
    userRepository.save(user);
    entityManager.flush();

    // Then - should be only one user with updated name
    assertThat(userRepository.count()).isEqualTo(1);
    User found = userRepository.findById(userId).orElseThrow();
    assertThat(found.getName()).isEqualTo("Updated 2");
  }

  @Test
  @DisplayName("Should generate unique ids for users")
  void save_MultipleUsers_ShouldHaveUniqueIds() {
    // Given & When
    User user1 = createUser("User 1");
    User user2 = createUser("User 2");
    User user3 = createUser("User 3");
    entityManager.flush();

    // Then
    assertThat(user1.getId()).isNotNull();
    assertThat(user2.getId()).isNotNull();
    assertThat(user3.getId()).isNotNull();
    assertThat(user1.getId()).isNotEqualTo(user2.getId());
    assertThat(user2.getId()).isNotEqualTo(user3.getId());
    assertThat(user1.getId()).isNotEqualTo(user3.getId());
  }

  @Test
  @DisplayName("Should save users with similar but different names")
  void save_SimilarNames_ShouldSaveAll() {
    // Given & When
    createUser("John");
    createUser("John Doe");
    createUser("John Smith");
    entityManager.flush();

    // Then
    List<User> allUsers = userRepository.findAll();
    assertThat(allUsers).hasSize(3);
    assertThat(allUsers)
        .extracting(User::getName)
        .containsExactlyInAnyOrder("John", "John Doe", "John Smith");
  }

  // Helper method
  private User createUser(String name) {
    User user = User.builder()
        .name(name)
        .build();
    return entityManager.persist(user);
  }
}