package com.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.entity.Booking;
import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.entity.enums.BookingStatus;
import com.booking.mapper.BookingMapper;
import com.booking.repository.BookingRepository;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.booking.web.dto.booking.BookingResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private UnitRepository unitRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private BookingMapper bookingMapper;

  @Mock
  private EventService eventService;

  @InjectMocks
  private BookingService bookingService;

  private User mockUser;
  private Unit mockUnit;
  private Booking mockBooking;
  private BookingResponse mockResponse;

  @BeforeEach
  void setUp() {
    mockUser = User.builder()
        .id(1L)
        .name("John Doe")
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
    mockBooking.setStartDate(LocalDate.of(2025, 10, 10));
    mockBooking.setEndDate(LocalDate.of(2025, 10, 15));
    mockBooking.setStatus(BookingStatus.PENDING);
    mockBooking.setCreatedAt(LocalDateTime.now());
    mockBooking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

    mockResponse = new BookingResponse(
        1L,
        1L,
        1L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 15),
        BookingStatus.PENDING,
        LocalDateTime.now(),
        LocalDateTime.now().plusMinutes(15)
    );
  }

  @Test
  @DisplayName("Should create booking successfully when unit is available")
  void create_WhenUnitAvailable_ShouldCreateBooking() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        1L,
        1L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 15)
    );

    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
    when(bookingRepository.findAll()).thenReturn(List.of()); // No existing bookings
    when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
    when(bookingMapper.toDto(mockBooking)).thenReturn(mockResponse);

    // When
    BookingResponse result = bookingService.create(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.unitId()).isEqualTo(1L);
    assertThat(result.userId()).isEqualTo(1L);
    assertThat(result.status()).isEqualTo(BookingStatus.PENDING);

    ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
    verify(bookingRepository).save(bookingCaptor.capture());

    Booking savedBooking = bookingCaptor.getValue();
    assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
    assertThat(savedBooking.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
  }

  @Test
  @DisplayName("Should throw exception when end date is before start date")
  void create_WhenEndDateBeforeStartDate_ShouldThrowException() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        1L,
        1L,
        LocalDate.of(2025, 10, 15),
        LocalDate.of(2025, 10, 10) // End before start
    );

    // When & Then
    assertThatThrownBy(() -> bookingService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("End date must be after start date");

    verify(bookingRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when end date equals start date")
  void create_WhenEndDateEqualsStartDate_ShouldThrowException() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        1L,
        1L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 10) // Same date
    );

    // When & Then
    assertThatThrownBy(() -> bookingService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("End date must be after start date");
  }

  @Test
  @DisplayName("Should throw exception when unit not found")
  void create_WhenUnitNotFound_ShouldThrowException() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        999L,
        1L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 15)
    );

    when(unitRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> bookingService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unit 999 not found");
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void create_WhenUserNotFound_ShouldThrowException() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        1L,
        999L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 15)
    );

    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> bookingService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User 999 not found");
  }

  @Test
  @DisplayName("Should throw exception when unit is not available")
  void create_WhenUnitNotAvailable_ShouldThrowException() {
    // Given
    BookingCreateRequest request = new BookingCreateRequest(
        1L,
        1L,
        LocalDate.of(2025, 10, 10),
        LocalDate.of(2025, 10, 15)
    );

    // Existing booking that overlaps
    Booking existingBooking = new Booking();
    existingBooking.setId(2L);
    existingBooking.setUnit(mockUnit);
    existingBooking.setStartDate(LocalDate.of(2025, 10, 12));
    existingBooking.setEndDate(LocalDate.of(2025, 10, 17));
    existingBooking.setStatus(BookingStatus.PAID);

    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
    when(bookingRepository.findAll()).thenReturn(List.of(existingBooking));

    // When & Then
    assertThatThrownBy(() -> bookingService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unit is not available for the selected dates");

    verify(bookingRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get booking by id successfully")
  void getById_WhenBookingExists_ShouldReturnBooking() {
    // Given
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(bookingMapper.toDto(mockBooking)).thenReturn(mockResponse);

    // When
    BookingResponse result = bookingService.getById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should throw exception when booking not found")
  void getById_WhenBookingNotFound_ShouldThrowException() {
    // Given
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> bookingService.getById(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Booking 999 not found");
  }

  @Test
  @DisplayName("Should cancel booking successfully")
  void cancel_WhenBookingPending_ShouldCancelBooking() {
    // Given
    mockBooking.setStatus(BookingStatus.PENDING);

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
    when(bookingMapper.toDto(mockBooking)).thenReturn(mockResponse);

    // When
    BookingResponse result = bookingService.cancel(1L);

    // Then
    assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    verify(bookingRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when cancelling already cancelled booking")
  void cancel_WhenAlreadyCancelled_ShouldThrowException() {
    // Given
    mockBooking.setStatus(BookingStatus.CANCELLED);

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

    // When & Then
    assertThatThrownBy(() -> bookingService.cancel(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Booking is already cancelled");
  }

  @Test
  @DisplayName("Should throw exception when cancelling paid booking")
  void cancel_WhenPaid_ShouldThrowException() {
    // Given
    mockBooking.setStatus(BookingStatus.PAID);

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

    // When & Then
    assertThatThrownBy(() -> bookingService.cancel(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Cannot cancel paid booking");
  }

  @Test
  @DisplayName("Should auto-cancel expired bookings")
  void cancelExpiredBookings_ShouldCancelOnlyExpiredPendingBookings() {
    // Given
    Booking expiredBooking1 = new Booking();
    expiredBooking1.setId(1L);
    expiredBooking1.setStatus(BookingStatus.PENDING);
    expiredBooking1.setExpiresAt(LocalDateTime.now().minusMinutes(1));

    Booking expiredBooking2 = new Booking();
    expiredBooking2.setId(2L);
    expiredBooking2.setStatus(BookingStatus.PENDING);
    expiredBooking2.setExpiresAt(LocalDateTime.now().minusMinutes(5));

    Booking notExpiredBooking = new Booking();
    notExpiredBooking.setId(3L);
    notExpiredBooking.setStatus(BookingStatus.PENDING);
    notExpiredBooking.setExpiresAt(LocalDateTime.now().plusMinutes(10));

    Booking paidBooking = new Booking();
    paidBooking.setId(4L);
    paidBooking.setStatus(BookingStatus.PAID);
    paidBooking.setExpiresAt(LocalDateTime.now().minusMinutes(1));

    when(bookingRepository.findAll()).thenReturn(
        List.of(expiredBooking1, expiredBooking2, notExpiredBooking, paidBooking)
    );

    // When
    bookingService.cancelExpiredBookings();

    // Then
    assertThat(expiredBooking1.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(expiredBooking2.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(notExpiredBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
    assertThat(paidBooking.getStatus()).isEqualTo(BookingStatus.PAID);
  }
}