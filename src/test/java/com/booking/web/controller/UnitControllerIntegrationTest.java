package com.booking.web.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.unit.UnitCreateRequest;
import com.booking.web.dto.unit.UnitUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("UnitController Integration Tests")
class UnitControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UnitRepository unitRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    unitRepository.deleteAll();
    userRepository.deleteAll();

    testUser = User.builder()
        .name("John Doe")
        .build();
    testUser = userRepository.save(testUser);
  }

  @Test
  @DisplayName("Should create unit successfully")
  void create_ValidRequest_ShouldReturnCreated() throws Exception {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "Modern flat in city center",
        testUser.getId()
    );

    // When & Then
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.numberOfRooms").value(2))
        .andExpect(jsonPath("$.accommodationType").value("FLAT"))
        .andExpect(jsonPath("$.floor").value(3))
        .andExpect(jsonPath("$.description").value("Modern flat in city center"))
        .andExpect(jsonPath("$.basePrice").value(100.00))
        .andExpect(jsonPath("$.markupPrice").value(115.00))
        .andExpect(jsonPath("$.createdById").value(testUser.getId()));
  }

  @Test
  @DisplayName("Should create apartments unit with correct markup")
  void create_ApartmentsUnit_ShouldCalculateMarkup() throws Exception {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        3,
        AccommodationType.APARTMENTS,
        5,
        new BigDecimal("80.00"),
        "Comfortable apartments near metro",
        testUser.getId()
    );

    // When & Then
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accommodationType").value("APARTMENTS"))
        .andExpect(jsonPath("$.basePrice").value(80.00))
        .andExpect(jsonPath("$.markupPrice").value(92.00));  // 80 * 1.15
  }

  @Test
  @DisplayName("Should create home unit successfully")
  void create_HomeUnit_ShouldWork() throws Exception {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        5,
        AccommodationType.HOME,
        1,
        new BigDecimal("250.00"),
        "Family home with garden",
        testUser.getId()
    );

    // When & Then
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accommodationType").value("HOME"))
        .andExpect(jsonPath("$.numberOfRooms").value(5))
        .andExpect(jsonPath("$.basePrice").value(250.00))
        .andExpect(jsonPath("$.markupPrice").value(287.50));  // 250 * 1.15
  }

  @Test
  @DisplayName("Should return 400 for invalid create request - null fields")
  void create_InvalidRequest_ShouldReturnBadRequest() throws Exception {
    // Given - invalid request with null values
    String invalidJson = """
        {
            "numberOfRooms": null,
            "accommodationType": null,
            "floor": null,
            "basePrice": null,
            "description": null,
            "createdById": null
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 for negative base price")
  void create_NegativeBasePrice_ShouldReturnBadRequest() throws Exception {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("-100.00"),  // Negative price
        "Test flat",
        testUser.getId()
    );

    // When & Then
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get unit by id successfully")
  void get_ExistingUnit_ShouldReturnUnit() throws Exception {
    // Given - create unit first
    UnitCreateRequest createRequest = new UnitCreateRequest(
        3,
        AccommodationType.APARTMENTS,
        2,
        new BigDecimal("80.00"),
        "Cozy apartments",
        testUser.getId()
    );

    String createResponse = mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long unitId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(get("/api/units/{id}", unitId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(unitId))
        .andExpect(jsonPath("$.numberOfRooms").value(3))
        .andExpect(jsonPath("$.accommodationType").value("APARTMENTS"))
        .andExpect(jsonPath("$.floor").value(2))
        .andExpect(jsonPath("$.description").value("Cozy apartments"))
        .andExpect(jsonPath("$.basePrice").value(80.00))
        .andExpect(jsonPath("$.markupPrice").value(92.00));
  }

  @Test
  @DisplayName("Should return 404 for non-existent unit")
  void get_NonExistentUnit_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/units/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should list units with pagination")
  void list_ShouldReturnPagedUnits() throws Exception {
    // Given - create multiple units
    createUnit(2, AccommodationType.FLAT, 1, "100.00", "Flat 1");
    createUnit(3, AccommodationType.APARTMENTS, 2, "80.00", "Apartments 1");
    createUnit(5, AccommodationType.HOME, 3, "250.00", "Home 1");

    // When & Then
    mockMvc.perform(get("/api/units")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content[0].numberOfRooms").exists())
        .andExpect(jsonPath("$.content[0].accommodationType").exists());
  }

  @Test
  @DisplayName("Should update unit successfully")
  void update_ExistingUnit_ShouldReturnUpdatedUnit() throws Exception {
    // Given - create unit first
    UnitCreateRequest createRequest = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "Original description",
        testUser.getId()
    );

    String createResponse = mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long unitId = objectMapper.readTree(createResponse).get("id").asLong();

    // Update request
    UnitUpdateRequest updateRequest = new UnitUpdateRequest(
        5,
        AccommodationType.HOME,
        1,
        new BigDecimal("500.00"),
        "Updated luxury home with amazing view"
    );

    // When & Then
    mockMvc.perform(put("/api/units/{id}", unitId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(unitId))
        .andExpect(jsonPath("$.numberOfRooms").value(5))
        .andExpect(jsonPath("$.accommodationType").value("HOME"))
        .andExpect(jsonPath("$.floor").value(1))
        .andExpect(jsonPath("$.basePrice").value(500.00))
        .andExpect(jsonPath("$.markupPrice").value(575.00))
        .andExpect(jsonPath("$.description").value("Updated luxury home with amazing view"));
  }

  @Test
  @DisplayName("Should recalculate markup when updating price")
  void update_WithNewPrice_ShouldRecalculateMarkup() throws Exception {
    // Given
    Long unitId = createUnit(2, AccommodationType.FLAT, 3, "100.00", "Test");

    UnitUpdateRequest updateRequest = new UnitUpdateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("200.00"),  // New price
        "Test flat"
    );

    // When & Then
    mockMvc.perform(put("/api/units/{id}", unitId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.basePrice").value(200.00))
        .andExpect(jsonPath("$.markupPrice").value(230.00));  // 200 * 1.15
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent unit")
  void update_NonExistentUnit_ShouldReturnNotFound() throws Exception {
    // Given
    UnitUpdateRequest updateRequest = new UnitUpdateRequest(
        2,
        AccommodationType.FLAT,
        5,
        new BigDecimal("200.00"),
        "Description"
    );

    // When & Then
    mockMvc.perform(put("/api/units/{id}", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 for invalid update request")
  void update_InvalidRequest_ShouldReturnBadRequest() throws Exception {
    // Given
    Long unitId = createUnit(2, AccommodationType.FLAT, 3, "100.00", "Test");

    UnitUpdateRequest invalidRequest = new UnitUpdateRequest(
        -5,  // Invalid negative rooms
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "Test"
    );

    // When & Then
    mockMvc.perform(put("/api/units/{id}", unitId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should delete unit successfully")
  void delete_ExistingUnit_ShouldReturnNoContent() throws Exception {
    // Given - create unit first
    UnitCreateRequest createRequest = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "To be deleted",
        testUser.getId()
    );

    String createResponse = mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long unitId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(delete("/api/units/{id}", unitId))
        .andExpect(status().isNoContent());

    // Verify unit is deleted
    mockMvc.perform(get("/api/units/{id}", unitId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent unit")
  void delete_NonExistentUnit_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/units/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle pagination correctly")
  void list_WithPagination_ShouldReturnCorrectPage() throws Exception {
    // Given - create 15 units
    for (int i = 0; i < 15; i++) {
      AccommodationType type = switch (i % 3) {
        case 0 -> AccommodationType.HOME;
        case 1 -> AccommodationType.FLAT;
        default -> AccommodationType.APARTMENTS;
      };
      createUnit(2, type, i + 1, "100.00", "Unit " + i);
    }

    // When & Then - request second page with size 5
    mockMvc.perform(get("/api/units")
            .param("page", "1")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.number").value(1));
  }

  @Test
  @DisplayName("Should test all accommodation types")
  void create_AllAccommodationTypes_ShouldWork() throws Exception {
    // Test HOME
    testAccommodationType(AccommodationType.HOME);

    // Test FLAT
    testAccommodationType(AccommodationType.FLAT);

    // Test APARTMENTS
    testAccommodationType(AccommodationType.APARTMENTS);
  }

  @Test
  @DisplayName("Should handle decimal prices correctly")
  void create_WithDecimalPrices_ShouldCalculateCorrectly() throws Exception {
    // Given - price with decimals
    UnitCreateRequest request = new UnitCreateRequest(
        1,
        AccommodationType.FLAT,
        1,
        new BigDecimal("50.50"),
        "Budget flat",
        testUser.getId()
    );

    // When & Then - markup should be 50.50 * 1.15 = 58.08
    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.basePrice").value(50.50))
        .andExpect(jsonPath("$.markupPrice").value(58.08));
  }

  @Test
  @DisplayName("Should complete full CRUD lifecycle")
  void fullCrudLifecycle_ShouldWorkCorrectly() throws Exception {
    // CREATE
    UnitCreateRequest createRequest = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "Test flat",
        testUser.getId()
    );

    String createResponse = mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long unitId = objectMapper.readTree(createResponse).get("id").asLong();

    // READ
    mockMvc.perform(get("/api/units/{id}", unitId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Test flat"))
        .andExpect(jsonPath("$.accommodationType").value("FLAT"));

    // UPDATE
    UnitUpdateRequest updateRequest = new UnitUpdateRequest(
        3,
        AccommodationType.APARTMENTS,
        5,
        new BigDecimal("150.00"),
        "Updated apartments"
    );

    mockMvc.perform(put("/api/units/{id}", unitId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Updated apartments"))
        .andExpect(jsonPath("$.numberOfRooms").value(3))
        .andExpect(jsonPath("$.accommodationType").value("APARTMENTS"));

    // DELETE
    mockMvc.perform(delete("/api/units/{id}", unitId))
        .andExpect(status().isNoContent());

    // VERIFY DELETION
    mockMvc.perform(get("/api/units/{id}", unitId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle different accommodation types in one list")
  void list_MixedAccommodationTypes_ShouldReturnAll() throws Exception {
    // Given - create units of all types
    createUnit(5, AccommodationType.HOME, 1, "250.00", "Family home");
    createUnit(2, AccommodationType.FLAT, 3, "100.00", "City flat");
    createUnit(3, AccommodationType.APARTMENTS, 5, "150.00", "Modern apartments");

    // When & Then
    mockMvc.perform(get("/api/units")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[*].accommodationType",
            containsInAnyOrder("HOME", "FLAT", "APARTMENTS")));
  }

  @Test
  @DisplayName("Should handle updating accommodation type")
  void update_ChangeAccommodationType_ShouldWork() throws Exception {
    // Given - create FLAT
    Long unitId = createUnit(2, AccommodationType.FLAT, 3, "100.00", "Original flat");

    // Update to HOME
    UnitUpdateRequest updateRequest = new UnitUpdateRequest(
        4,
        AccommodationType.HOME,
        1,
        new BigDecimal("200.00"),
        "Now it's a home"
    );

    // When & Then
    mockMvc.perform(put("/api/units/{id}", unitId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accommodationType").value("HOME"))
        .andExpect(jsonPath("$.numberOfRooms").value(4));
  }

  // Helper methods
  private Long createUnit(int rooms, AccommodationType type, int floor,
      String price, String description) throws Exception {
    UnitCreateRequest request = new UnitCreateRequest(
        rooms, type, floor, new BigDecimal(price), description, testUser.getId()
    );

    String response = mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }

  private void testAccommodationType(AccommodationType type) throws Exception {
    UnitCreateRequest request = new UnitCreateRequest(
        2, type, 3, new BigDecimal("100.00"),
        "Test " + type.name(), testUser.getId()
    );

    mockMvc.perform(post("/api/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accommodationType").value(type.name()));
  }
}