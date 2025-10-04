package com.booking.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.repository.UserRepository;
import com.booking.web.dto.user.UserCreateRequest;
import com.booking.web.dto.user.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create user successfully")
  void create_ValidRequest_ShouldReturnCreated() throws Exception {
    // Given
    UserCreateRequest request = new UserCreateRequest("John Doe");

    // When & Then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("John Doe"));
  }

  @Test
  @DisplayName("Should create user with different names")
  void create_DifferentNames_ShouldWork() throws Exception {
    // Test with simple name
    UserCreateRequest request1 = new UserCreateRequest("Alice");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Alice"));

    // Test with full name
    UserCreateRequest request2 = new UserCreateRequest("Bob Smith Jr.");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Bob Smith Jr."));
  }

  @Test
  @DisplayName("Should return 400 for invalid create request - blank name")
  void create_BlankName_ShouldReturnBadRequest() throws Exception {
    // Given - blank name
    UserCreateRequest request = new UserCreateRequest("   ");

    // When & Then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 for null name")
  void create_NullName_ShouldReturnBadRequest() throws Exception {
    // Given
    String invalidJson = """
        {
            "name": null
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get user by id successfully")
  void get_ExistingUser_ShouldReturnUser() throws Exception {
    // Given - create user first
    UserCreateRequest createRequest = new UserCreateRequest("Jane Smith");

    String createResponse = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long userId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.name").value("Jane Smith"));
  }

  @Test
  @DisplayName("Should return 404 for non-existent user")
  void get_NonExistentUser_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/users/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should update user successfully")
  void update_ExistingUser_ShouldReturnUpdatedUser() throws Exception {
    // Given - create user first
    UserCreateRequest createRequest = new UserCreateRequest("Old Name");

    String createResponse = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long userId = objectMapper.readTree(createResponse).get("id").asLong();

    // Update request
    UserUpdateRequest updateRequest = new UserUpdateRequest("New Name");

    // When & Then
    mockMvc.perform(put("/api/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.name").value("New Name"));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent user")
  void update_NonExistentUser_ShouldReturnNotFound() throws Exception {
    // Given
    UserUpdateRequest updateRequest = new UserUpdateRequest("Test User");

    // When & Then
    mockMvc.perform(put("/api/users/{id}", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 for invalid update request")
  void update_BlankName_ShouldReturnBadRequest() throws Exception {
    // Given - create user first
    Long userId = createUser("Test User");

    UserUpdateRequest invalidRequest = new UserUpdateRequest("   ");  // Blank name

    // When & Then
    mockMvc.perform(put("/api/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should delete user successfully")
  void delete_ExistingUser_ShouldReturnNoContent() throws Exception {
    // Given - create user first
    UserCreateRequest createRequest = new UserCreateRequest("To Delete");

    String createResponse = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long userId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(delete("/api/users/{id}", userId))
        .andExpect(status().isNoContent());

    // Verify user is deleted
    mockMvc.perform(get("/api/users/{id}", userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent user")
  void delete_NonExistentUser_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/users/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle complete user lifecycle")
  void completeLifecycle_CreateUpdateDelete_ShouldWorkCorrectly() throws Exception {
    // CREATE
    UserCreateRequest createRequest = new UserCreateRequest("Lifecycle Test User");

    String createResponse = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long userId = objectMapper.readTree(createResponse).get("id").asLong();

    // READ
    mockMvc.perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Lifecycle Test User"));

    // UPDATE
    UserUpdateRequest updateRequest = new UserUpdateRequest("Updated User Name");

    mockMvc.perform(put("/api/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated User Name"));

    // DELETE
    mockMvc.perform(delete("/api/users/{id}", userId))
        .andExpect(status().isNoContent());

    // VERIFY DELETION
    mockMvc.perform(get("/api/users/{id}", userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle multiple users creation")
  void create_MultipleUsers_ShouldWork() throws Exception {
    // Create 5 users
    for (int i = 1; i <= 5; i++) {
      UserCreateRequest request = new UserCreateRequest("User " + i);

      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("User " + i))
          .andExpect(jsonPath("$.id").exists());
    }
  }

  @Test
  @DisplayName("Should handle long names")
  void create_LongName_ShouldWork() throws Exception {
    // Given - name with 255 characters (database limit)
    String longName = "A".repeat(255);
    UserCreateRequest request = new UserCreateRequest(longName);

    // When & Then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value(longName));
  }

  @Test
  @DisplayName("Should handle special characters in name")
  void create_SpecialCharacters_ShouldWork() throws Exception {
    // Given
    String nameWithSpecialChars = "Jean-François O'Brien";
    UserCreateRequest request = new UserCreateRequest(nameWithSpecialChars);

    // When & Then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value(nameWithSpecialChars));
  }

  // Helper method
  private Long createUser(String name) throws Exception {
    UserCreateRequest request = new UserCreateRequest(name);

    String response = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }
}