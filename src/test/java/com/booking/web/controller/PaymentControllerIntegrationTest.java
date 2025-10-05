package com.booking.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.repository.BookingRepository;
import com.booking.repository.PaymentRepository;
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
@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

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
    paymentRepository.deleteAll();
    bookingRepository.deleteAll();
    unitRepository.deleteAll();
    userRepository.deleteAll();

    testUser = User.builder()
        .name("Payment Test User")
        .build();
    testUser = userRepository.save(testUser);

    testUnit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test flat for payment")
        .createdBy(testUser)
        .build();
    testUnit = unitRepository.save(testUnit);
  }

  @Test
  @DisplayName("Should process payment successfully")
  void processPayment_ValidBooking_ShouldReturnCreated() throws Exception {
    // Given - create booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15) // 5 nights
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // When & Then - process payment
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.bookingId").value(bookingId))
        .andExpect(jsonPath("$.amount").value(575.00)) // 5 nights * 115
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.paidAt").exists())
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Should calculate correct amount for different stays")
  void processPayment_DifferentStays_ShouldCalculateCorrectly() throws Exception {
    // Given - 4 nights booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 1),
        LocalDate.of(2025, 11, 5) // 4 nights
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // When & Then - amount should be 4 * 115 = 460
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amount").value(460.00));
  }

  @Test
  @DisplayName("Should return 404 for non-existent booking")
  void processPayment_NonExistentBooking_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/payments/booking/{bookingId}", 999999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 for already paid booking")
  void processPayment_AlreadyPaid_ShouldReturnBadRequest() throws Exception {
    // Given - create and pay for booking
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

    // Pay first time
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated());

    // When & Then - try to pay again
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 for cancelled booking")
  void processPayment_CancelledBooking_ShouldReturnBadRequest() throws Exception {
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

    // Cancel booking
    mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId))
        .andExpect(status().isOk());

    // When & Then - try to pay for cancelled booking
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get payment by ID")
  void getById_ExistingPayment_ShouldReturnPayment() throws Exception {
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

    String paymentResponse = mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long paymentId = objectMapper.readTree(paymentResponse).get("id").asLong();

    // When & Then
    mockMvc.perform(get("/api/payments/{id}", paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.bookingId").value(bookingId))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @DisplayName("Should return 404 for non-existent payment")
  void getById_NonExistent_ShouldReturnNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/payments/{id}", 999999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should get payment by booking ID")
  void getByBookingId_ExistingPayment_ShouldReturnPayment() throws Exception {
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
    mockMvc.perform(get("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingId").value(bookingId))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @DisplayName("Should return 404 when getting payment for booking without payment")
  void getByBookingId_NoPayment_ShouldReturnNotFound() throws Exception {
    // Given - create booking without payment
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

    // When & Then
    mockMvc.perform(get("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should calculate amount for single night stay")
  void processPayment_SingleNight_ShouldCalculateCorrectly() throws Exception {
    // Given - 1 night booking
    BookingCreateRequest bookingRequest = new BookingCreateRequest(
        testUnit.getId(),
        testUser.getId(),
        LocalDate.of(2025, 11, 1),
        LocalDate.of(2025, 11, 2) // 1 night
    );

    String bookingResponse = mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookingRequest)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // When & Then - amount should be 1 * 115 = 115
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amount").value(115.00));
  }

  @Test
  @DisplayName("Should update booking status to PAID after payment")
  void processPayment_ShouldUpdateBookingStatus() throws Exception {
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

    // Verify initial status is PENDING
    mockMvc.perform(get("/api/bookings/{id}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));

    // When - process payment
    mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated());

    // Then - verify status changed to PAID
    mockMvc.perform(get("/api/bookings/{id}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));
  }

  @Test
  @DisplayName("Should handle multiple bookings and payments")
  void processPayment_MultipleBookings_ShouldHandleAll() throws Exception {
    // Given - create 3 bookings
    for (int i = 0; i < 3; i++) {
      BookingCreateRequest bookingRequest = new BookingCreateRequest(
          testUnit.getId(),
          testUser.getId(),
          LocalDate.of(2025 + i, 11, 1),  // Change year instead of month
          LocalDate.of(2025 + i, 11, 5)
      );

      String bookingResponse = mockMvc.perform(post("/api/bookings")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(bookingRequest)))
          .andExpect(status().isCreated())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

      // Pay for each booking
      mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.bookingId").value(bookingId));
    }
  }

  @Test
  @DisplayName("Complete payment workflow should work correctly")
  void completePaymentWorkflow_ShouldSucceed() throws Exception {
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
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

    // 2. Process payment
    String paymentResponse = mockMvc.perform(post("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long paymentId = objectMapper.readTree(paymentResponse).get("id").asLong();

    // 3. Verify booking status updated
    mockMvc.perform(get("/api/bookings/{id}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));

    // 4. Verify payment exists
    mockMvc.perform(get("/api/payments/{id}", paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingId").value(bookingId));

    // 5. Verify payment can be retrieved by booking ID
    mockMvc.perform(get("/api/payments/booking/{bookingId}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId));
  }
}