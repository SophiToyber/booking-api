package com.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.entity.Booking;
import com.booking.entity.Payment;
import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.entity.enums.BookingStatus;
import com.booking.entity.enums.EventType;
import com.booking.entity.enums.PaymentStatus;
import com.booking.mapper.PaymentMapper;
import com.booking.repository.BookingRepository;
import com.booking.repository.PaymentRepository;
import com.booking.web.dto.payment.PaymentResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private EventService eventService;

  @InjectMocks
  private PaymentService paymentService;

  private User mockUser;
  private Unit mockUnit;
  private Booking mockBooking;
  private Payment mockPayment;
  private PaymentResponse mockResponse;

  @BeforeEach
  void setUp() {
    mockUser = User.builder()
        .id(1L)
        .name("Test User")
        .build();

    mockUnit = Unit.builder()
        .id(1L)
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test flat")
        .createdBy(mockUser)
        .build();

    mockBooking = new Booking();
    mockBooking.setId(1L);
    mockBooking.setUnit(mockUnit);
    mockBooking.setUser(mockUser);
    mockBooking.setStartDate(LocalDate.of(2025, 11, 1));
    mockBooking.setEndDate(LocalDate.of(2025, 11, 5)); // 4 nights
    mockBooking.setStatus(BookingStatus.PENDING);
    mockBooking.setCreatedAt(LocalDateTime.now());
    mockBooking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

    mockPayment = Payment.builder()
        .id(1L)
        .booking(mockBooking)
        .amount(new BigDecimal("460.00")) // 4 nights * 115
        .status(PaymentStatus.COMPLETED)
        .paidAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .build();

    mockResponse = new PaymentResponse(
        1L,
        1L,
        new BigDecimal("460.00"),
        PaymentStatus.COMPLETED,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  @Test
  @DisplayName("Should process payment successfully")
  void processPayment_ValidBooking_ShouldCreatePayment() {
    // Given
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
    when(paymentMapper.toDto(mockPayment)).thenReturn(mockResponse);

    // When
    PaymentResponse result = paymentService.processPayment(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.bookingId()).isEqualTo(1L);
    assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("460.00"));
    assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);

    // Verify booking status updated to PAID
    assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.PAID);

    // Verify payment saved
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    Payment savedPayment = paymentCaptor.getValue();
    assertThat(savedPayment.getBooking()).isEqualTo(mockBooking);
    assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("460.00"));
    assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

    // Verify event logged
    verify(eventService).logEvent(
        eq(EventType.PAYMENT_COMPLETED),
        eq("Payment"),
        eq(1L),
        any(String.class)
    );
  }

  @Test
  @DisplayName("Should calculate correct amount for multiple nights")
  void processPayment_MultipleNights_ShouldCalculateCorrectAmount() {
    // Given - 4 nights * 115 = 460
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
    when(paymentMapper.toDto(any())).thenReturn(mockResponse);

    // When
    paymentService.processPayment(1L);

    // Then
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(captor.capture());
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("460.00"));
  }

  @Test
  @DisplayName("Should throw exception when booking not found")
  void processPayment_BookingNotFound_ShouldThrowException() {
    // Given
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> paymentService.processPayment(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Booking 999 not found");

    verify(paymentRepository, never()).save(any());
    verify(eventService, never()).logEvent(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should throw exception when booking is cancelled")
  void processPayment_CancelledBooking_ShouldThrowException() {
    // Given
    mockBooking.setStatus(BookingStatus.CANCELLED);
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

    // When & Then
    assertThatThrownBy(() -> paymentService.processPayment(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Cannot pay for cancelled booking");

    verify(paymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when booking is already paid")
  void processPayment_AlreadyPaid_ShouldThrowException() {
    // Given
    mockBooking.setStatus(BookingStatus.PAID);
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

    // When & Then
    assertThatThrownBy(() -> paymentService.processPayment(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Booking is already paid");

    verify(paymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception and cancel booking when expired")
  void processPayment_ExpiredBooking_ShouldCancelAndThrowException() {
    // Given - booking expired 1 minute ago
    mockBooking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

    // When & Then
    assertThatThrownBy(() -> paymentService.processPayment(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Booking has expired and was cancelled");

    // Verify booking was cancelled
    assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

    // Verify event logged for expiration
    verify(eventService).logEvent(
        eq(EventType.BOOKING_EXPIRED),
        eq("Booking"),
        eq(1L),
        any(String.class)
    );

    verify(paymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get payment by booking ID")
  void getByBookingId_ExistingPayment_ShouldReturnPayment() {
    // Given
    when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(mockPayment));
    when(paymentMapper.toDto(mockPayment)).thenReturn(mockResponse);

    // When
    PaymentResponse result = paymentService.getByBookingId(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.bookingId()).isEqualTo(1L);
    verify(paymentRepository).findByBookingId(1L);
  }

  @Test
  @DisplayName("Should throw exception when payment not found by booking ID")
  void getByBookingId_NotFound_ShouldThrowException() {
    // Given
    when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> paymentService.getByBookingId(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Payment for booking 999 not found");
  }

  @Test
  @DisplayName("Should get payment by ID")
  void getById_ExistingPayment_ShouldReturnPayment() {
    // Given
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
    when(paymentMapper.toDto(mockPayment)).thenReturn(mockResponse);

    // When
    PaymentResponse result = paymentService.getById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    verify(paymentRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when payment not found by ID")
  void getById_NotFound_ShouldThrowException() {
    // Given
    when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> paymentService.getById(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Payment 999 not found");
  }

  @Test
  @DisplayName("Should calculate amount for single night")
  void processPayment_SingleNight_ShouldCalculateCorrectAmount() {
    // Given - 1 night
    mockBooking.setStartDate(LocalDate.of(2025, 11, 1));
    mockBooking.setEndDate(LocalDate.of(2025, 11, 2));

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
    when(paymentMapper.toDto(any())).thenReturn(mockResponse);

    // When
    paymentService.processPayment(1L);

    // Then
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(captor.capture());
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("115.00"));
  }

  @Test
  @DisplayName("Should calculate amount for week stay")
  void processPayment_WeekStay_ShouldCalculateCorrectAmount() {
    // Given - 7 nights
    mockBooking.setStartDate(LocalDate.of(2025, 11, 1));
    mockBooking.setEndDate(LocalDate.of(2025, 11, 8));

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
    when(paymentMapper.toDto(any())).thenReturn(mockResponse);

    // When
    paymentService.processPayment(1L);

    // Then
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(captor.capture());
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("805.00")); // 7 * 115
  }
}