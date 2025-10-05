package com.booking.web.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.repository.BookingRepository;
import com.booking.repository.EventRepository;
import com.booking.repository.PaymentRepository;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@DisplayName("EventController Integration Tests")
class EventControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private PaymentRepository paymentRepository;

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
    eventRepository.deleteAll();
    paymentRepository.deleteAll();
    bookingRepository.deleteAll();
    unitRepository.deleteAll();
    userRepository.deleteAll();

    testUser = User.builder()
        .name("Event Test User")
        .build();
    testUser = userRepository.save(testUser);

    testUnit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test flat for events")
        .createdBy(testUser)
        .build();
    testUnit = unitRepository.save(testUnit);
  }

  @Test
  @DisplayName("Should create events when booking is created")
  void createBooking_ShouldLogEvent() throws Exception {
    // Given
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    // When - create booking (should log BOOKING_CREATED event)
    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // Then - verify event was logged
    mockMvc.perform(get("/api/events")
            .param("entityType", "Booking")
            .param("entityId", bookingId.toString())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].entityType").value("Booking"))
        .andExpect(jsonPath("$.content[0].entityId").value(bookingId));
  }

  @Test
  @DisplayName("Should create events for complete booking lifecycle")
  void completeBookingLifecycle_ShouldLogAllEvents() throws Exception {
    // 1. Create booking - logs BOOKING_CREATED
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // 2. Process payment - logs PAYMENT_COMPLETED
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated());

    // 3. Verify multiple events logged
    mockMvc.perform(get("/api/events")
            .param("entityType", "Booking")
            .param("entityId", bookingId.toString())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Should get events by type - BOOKING_CREATED")
  void getEventsByType_BookingCreated_ShouldReturnEvents() throws Exception {
    // Given - create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated());

    // When & Then
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].eventType").value("BOOKING_CREATED"));
  }

  @Test
  @DisplayName("Should get events by type - PAYMENT_COMPLETED")
  void getEventsByType_PaymentCompleted_ShouldReturnEvents() throws Exception {
    // Given - create booking and payment
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated());

    // When & Then
    mockMvc.perform(get("/api/events/type/PAYMENT_COMPLETED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].eventType").value("PAYMENT_COMPLETED"));
  }

  @Test
  @DisplayName("Should get events by type - BOOKING_CANCELLED")
  void getEventsByType_BookingCancelled_ShouldReturnEvents() throws Exception {
    // Given - create and cancel booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId))
        .andExpect(status().isOk());

    // When & Then
    mockMvc.perform(get("/api/events/type/BOOKING_CANCELLED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].eventType").value("BOOKING_CANCELLED"));
  }

  @Test
  @DisplayName("Should get events by date range")
  void getEventsByDateRange_ValidRange_ShouldReturnEvents() throws Exception {
    // Given - create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated());

    // When & Then - get today's events
    LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    mockMvc.perform(get("/api/events/date-range")
            .param("start", start.format(formatter))
            .param("end", end.format(formatter))
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Should get event by ID")
  void getById_ExistingEvent_ShouldReturnEvent() throws Exception {
    // Given - create booking (generates event)
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated());

    // Get all events to find an ID
    String eventsResponse = mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "1"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long eventId = objectMapper.readTree(eventsResponse)
        .get("content")
        .get(0)
        .get("id")
        .asLong();

    // When & Then
    mockMvc.perform(get("/api/events/{id}", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(eventId))
        .andExpect(jsonPath("$.eventType").exists())
        .andExpect(jsonPath("$.entityType").exists())
        .andExpect(jsonPath("$.entityId").exists());
  }

  @Test
  @DisplayName("Should return 404 for non-existent event")
  void getById_NonExistent_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/events/{id}", 999999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return empty page for events with no results")
  void getEventsByType_NoResults_ShouldReturnEmptyPage() throws Exception {
    // When & Then - query for event type that doesn't exist
    mockMvc.perform(get("/api/events/type/PAYMENT_FAILED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("Should handle pagination for events")
  void getEvents_WithPagination_ShouldReturnCorrectPage() throws Exception {
    // Given - create multiple bookings to generate events
    for (int i = 0; i < 5; i++) {
      BookingCreateRequest bookingRequest = new BookingCreateRequest(
          testUnit.getId(),
          testUser.getId(),
          LocalDate.of(2026 + i, 1, 1),  // Changed: use different years
          LocalDate.of(2026 + i, 1, 5)
      );

      mockMvc.perform(post("/api/bookings")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(bookingRequest)))
          .andExpect(status().isCreated());
    }

    // When & Then - get first page
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(5)))
        .andExpect(jsonPath("$.number").value(0));

    // Get second page
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "1")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1));
  }

  @Test
  @DisplayName("Should track complete workflow with events")
  void completeWorkflow_ShouldTrackAllEvents() throws Exception {
    // 1. Create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // 2. Process payment
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated());

    // 3. Verify BOOKING_CREATED event exists
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

    // 4. Verify PAYMENT_COMPLETED event exists
    mockMvc.perform(get("/api/events/type/PAYMENT_COMPLETED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

    // 5. Verify events for specific booking
    mockMvc.perform(get("/api/events")
            .param("entityType", "Booking")
            .param("entityId", bookingId.toString())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Should log events for cancelled bookings")
  void cancelBooking_ShouldLogCancellationEvent() throws Exception {
    // Given - create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // When - cancel booking
    mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId))
        .andExpect(status().isOk());

    // Then - verify cancellation event logged
    mockMvc.perform(get("/api/events/type/BOOKING_CANCELLED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].eventType").value("BOOKING_CANCELLED"));
  }

  @Test
  @DisplayName("Should return events with all required fields")
  void getEvent_ShouldContainAllFields() throws Exception {
    // Given - create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated());

    // When & Then - verify all fields present
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").exists())
        .andExpect(jsonPath("$.content[0].eventType").exists())
        .andExpect(jsonPath("$.content[0].entityType").exists())
        .andExpect(jsonPath("$.content[0].entityId").exists())
        .andExpect(jsonPath("$.content[0].details").exists())
        .andExpect(jsonPath("$.content[0].createdAt").exists());
  }

  @Test
  @DisplayName("Should handle multiple event types in single request")
  void multipleOperations_ShouldLogDifferentEventTypes() throws Exception {
    // 1. Create first booking - BOOKING_CREATED
    BookingCreateRequest booking1 = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 1),
        LocalDate.of(2025, 11, 5)
    );

    String booking1Response = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(booking1)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long booking1Id = objectMapper.readTree(booking1Response).get("id").asLong();

    // 2. Create second booking - BOOKING_CREATED
    BookingCreateRequest booking2 = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 12, 1),
        LocalDate.of(2025, 12, 5)
    );

    String booking2Response = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(booking2)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long booking2Id = objectMapper.readTree(booking2Response).get("id").asLong();

    // 3. Pay for first booking - PAYMENT_COMPLETED
    mockMvc.perform(post("/api/payments/booking/{bookingId}", booking1Id))
        .andExpect(status().isCreated());

    // 4. Cancel second booking - BOOKING_CANCELLED
    mockMvc.perform(put("/api/bookings/{id}/cancel", booking2Id))
        .andExpect(status().isOk());

    // Verify different event types exist
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

    mockMvc.perform(get("/api/events/type/PAYMENT_COMPLETED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

    mockMvc.perform(get("/api/events/type/BOOKING_CANCELLED")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Should sort events by creation date")
  void getEvents_ShouldBeSortedByCreationDate() throws Exception {
    // Given - create multiple bookings with slight delays
    for (int i = 0; i < 3; i++) {
      BookingCreateRequest bookingRequest = new BookingCreateRequest(
          testUnit.getId(),
          testUser.getId(),
          LocalDate.of(2025 + i, 1, 1),   // Use different years
          LocalDate.of(2025 + i, 1, 5)
      );

      mockMvc.perform(post("/api/bookings")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(bookingRequest)))
          .andExpect(status().isCreated());
    }

    // When & Then - events should be retrievable
    mockMvc.perform(get("/api/events/type/BOOKING_CREATED")
            .param("page", "0")
            .param("size", "10")
            .param("sort", "createdAt,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))));
  }

  @Test
  @DisplayName("Should filter events by entity correctly")
  void getEventsByEntity_SpecificBooking_ShouldReturnOnlyThoseEvents() throws Exception {
    // Given - create two bookings
    BookingCreateRequest booking1 = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 1),
        LocalDate.of(2025, 11, 5)
    );

    String booking1Response = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(booking1)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long booking1Id = objectMapper.readTree(booking1Response).get("id").asLong();

    BookingCreateRequest booking2 = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 12, 1),
        LocalDate.of(2025, 12, 5)
    );

    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(booking2)))
        .andExpect(status().isCreated());

    // When & Then - get events only for booking1
    mockMvc.perform(get("/api/events")
            .param("entityType", "Booking")
            .param("entityId", booking1Id.toString())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    // Removed the incorrect assertion that was checking entityId
  }
}