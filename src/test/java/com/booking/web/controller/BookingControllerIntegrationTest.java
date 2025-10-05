package com.booking.web.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.repository.BookingRepository;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("BookingController Integration Tests")
class BookingControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private UnitRepository unitRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;
  private Unit testUnit;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();
    unitRepository.deleteAll();
    userRepository.deleteAll();

    testUser = User.builder()
        .name("Test User")
        .build();
    testUser = userRepository.save(testUser);

    testUnit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test flat for booking")
        .createdBy(testUser)
        .build();
    testUnit = unitRepository.save(testUnit);
  }

  @Test
  @DisplayName("Should create booking successfully")
  void create_ValidRequest_ShouldReturnCreated() throws Exception {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.unitId").value(testUnit.getId()))
        .andExpect(jsonPath("$.userId").value(testUser.getId()))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.expiresAt").exists());
  }

  @Test
  @DisplayName("Should return 400 for invalid dates - end before start")
  void create_EndDateBeforeStartDate_ShouldReturnBadRequest() throws Exception {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(1) // End before start
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 when unit not found")
  void create_UnitNotFound_ShouldReturnNotFound() throws Exception {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        999L, // Non-existent unit
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when user not found")
  void create_UserNotFound_ShouldReturnNotFound() throws Exception {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        testUnit.getId(),
        999L, // Non-existent user
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 409 when unit is already booked for the dates")
  void create_UnitNotAvailable_ShouldReturnConflict() throws Exception {
    // Given - create first booking
    BookingCreateRequest firstRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated());

    // Try to create overlapping booking
    BookingCreateRequest overlappingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(3), // Overlaps with first booking
        LocalDate.now().plusDays(7)
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(overlappingRequest)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Should get booking by id successfully")
  void get_ExistingBooking_ShouldReturnBooking() throws Exception {
    // Given - create booking first
    BookingCreateRequest createRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    String createResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(get("/api/bookings/{id}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookingId))
        .andExpect(jsonPath("$.unitId").value(testUnit.getId()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("Should return 404 for non-existent booking")
  void get_NonExistentBooking_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/bookings/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should cancel booking successfully")
  void cancel_PendingBooking_ShouldReturnCancelled() throws Exception {
    // Given - create booking
    BookingCreateRequest createRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    String createResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(createResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookingId))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("Should allow booking unit after previous booking is cancelled")
  void create_AfterCancellation_ShouldAllowNewBooking() throws Exception {
    // Given - create and cancel first booking
    BookingCreateRequest firstRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    String firstResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long firstBookingId = objectMapper.readTree(firstResponse).get("id").asLong();

    mockMvc.perform(put("/api/bookings/{id}/cancel", firstBookingId))
        .andExpect(status().isOk());

    // When - create new booking for same dates
    BookingCreateRequest secondRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    // Then - should succeed
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("Should allow non-overlapping bookings for same unit")
  void create_NonOverlappingBookings_ShouldSucceed() throws Exception {
    // Given - create first booking
    BookingCreateRequest firstRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated());

    // When - create second booking after first
    BookingCreateRequest secondRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(6), // Starts after first ends
        LocalDate.now().plusDays(10)
    );

    // Then - should succeed
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("Should validate expiration time is set correctly")
  void create_ShouldSetExpirationTime() throws Exception {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    );

    // When & Then
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.expiresAt", notNullValue()));
  }
}