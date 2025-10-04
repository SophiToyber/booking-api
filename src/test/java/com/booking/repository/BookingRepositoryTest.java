package com.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.entity.Booking;
import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@DisplayName("BookingRepository Tests")
class BookingRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookingRepository bookingRepository;

  private User testUser;
  private Unit testUnit;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();

    testUser = User.builder()
        .name("Test User")
        .build();
    testUser = entityManager.persistAndFlush(testUser);

    testUnit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test flat")
        .createdBy(testUser)
        .build();
    testUnit = entityManager.persistAndFlush(testUnit);
  }

  @Test
  @DisplayName("Should save and find booking by id")
  void save_ShouldPersistBooking() {
    // Given
    Booking booking = new Booking();
    booking.setUnit(testUnit);
    booking.setUser(testUser);
    booking.setStartDate(LocalDate.now().plusDays(1));
    booking.setEndDate(LocalDate.now().plusDays(5));
    booking.setStatus(BookingStatus.PENDING);
    booking.setCreatedAt(LocalDateTime.now());
    booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

    // When
    Booking saved = bookingRepository.save(booking);
    entityManager.flush();
    entityManager.clear();

    // Then
    Optional<Booking> found = bookingRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getUnit().getId()).isEqualTo(testUnit.getId());
    assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    assertThat(found.get().getStatus()).isEqualTo(BookingStatus.PENDING);
    assertThat(found.get().getStartDate()).isEqualTo(LocalDate.now().plusDays(1));
    assertThat(found.get().getEndDate()).isEqualTo(LocalDate.now().plusDays(5));
    assertThat(found.get().getCreatedAt()).isNotNull();
    assertThat(found.get().getExpiresAt()).isNotNull();
  }

  @Test
  @DisplayName("Should return empty when booking not found")
  void findById_WhenNotExists_ShouldReturnEmpty() {
    // When
    Optional<Booking> found = bookingRepository.findById(999L);

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find all bookings")
  void findAll_ShouldReturnAllBookings() {
    // Given - create multiple bookings
    createBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), BookingStatus.PENDING);
    createBooking(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10), BookingStatus.PAID);
    createBooking(LocalDate.now().plusDays(11), LocalDate.now().plusDays(15),
        BookingStatus.CANCELLED);

    entityManager.flush();

    // When
    List<Booking> bookings = bookingRepository.findAll();

    // Then
    assertThat(bookings).hasSize(3);
    assertThat(bookings)
        .extracting(Booking::getStatus)
        .containsExactlyInAnyOrder(BookingStatus.PENDING, BookingStatus.PAID,
            BookingStatus.CANCELLED);
  }

  @Test
  @DisplayName("Should update booking status")
  void update_ShouldChangeStatus() {
    // Given
    Booking booking = createBooking(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5),
        BookingStatus.PENDING
    );
    entityManager.flush();
    entityManager.clear();

    // When
    Booking found = bookingRepository.findById(booking.getId()).orElseThrow();
    found.setStatus(BookingStatus.PAID);
    bookingRepository.save(found);
    entityManager.flush();
    entityManager.clear();

    // Then
    Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(BookingStatus.PAID);
  }

  @Test
  @DisplayName("Should delete booking")
  void delete_ShouldRemoveBooking() {
    // Given
    Booking booking = createBooking(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5),
        BookingStatus.PENDING
    );
    Long bookingId = booking.getId();
    entityManager.flush();
    entityManager.clear();

    // When
    bookingRepository.deleteById(bookingId);
    entityManager.flush();

    // Then
    Optional<Booking> found = bookingRepository.findById(bookingId);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should save booking with all statuses")
  void save_AllStatuses_ShouldWork() {
    // Given & When
    Booking pending = createBooking(LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5), BookingStatus.PENDING);
    Booking paid = createBooking(LocalDate.now().plusDays(6),
        LocalDate.now().plusDays(10), BookingStatus.PAID);
    Booking cancelled = createBooking(LocalDate.now().plusDays(11),
        LocalDate.now().plusDays(15), BookingStatus.CANCELLED);
    Booking booked = createBooking(LocalDate.now().plusDays(16),
        LocalDate.now().plusDays(20), BookingStatus.BOOKED);

    entityManager.flush();

    // Then
    assertThat(bookingRepository.findById(pending.getId()))
        .isPresent()
        .get()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.PENDING);

    assertThat(bookingRepository.findById(paid.getId()))
        .isPresent()
        .get()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.PAID);

    assertThat(bookingRepository.findById(cancelled.getId()))
        .isPresent()
        .get()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.CANCELLED);

    assertThat(bookingRepository.findById(booked.getId()))
        .isPresent()
        .get()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.BOOKED);
  }

  @Test
  @DisplayName("Should maintain relationship with Unit and User")
  void findById_ShouldLoadRelationships() {
    // Given
    Booking booking = createBooking(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5),
        BookingStatus.PENDING
    );
    entityManager.flush();
    entityManager.clear();

    // When
    Booking found = bookingRepository.findById(booking.getId()).orElseThrow();

    // Then
    assertThat(found.getUnit()).isNotNull();
    assertThat(found.getUnit().getId()).isEqualTo(testUnit.getId());
    assertThat(found.getUser()).isNotNull();
    assertThat(found.getUser().getId()).isEqualTo(testUser.getId());
  }

  @Test
  @DisplayName("Should save booking with dates")
  void save_WithDates_ShouldPersistCorrectly() {
    // Given
    LocalDate startDate = LocalDate.of(2025, 12, 1);
    LocalDate endDate = LocalDate.of(2025, 12, 10);

    Booking booking = new Booking();
    booking.setUnit(testUnit);
    booking.setUser(testUser);
    booking.setStartDate(startDate);
    booking.setEndDate(endDate);
    booking.setStatus(BookingStatus.PENDING);
    booking.setCreatedAt(LocalDateTime.now());
    booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

    // When
    Booking saved = bookingRepository.save(booking);
    entityManager.flush();
    entityManager.clear();

    // Then
    Booking found = bookingRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStartDate()).isEqualTo(startDate);
    assertThat(found.getEndDate()).isEqualTo(endDate);
  }

  @Test
  @DisplayName("Should save booking with expiration time")
  void save_WithExpirationTime_ShouldPersist() {
    // Given
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime expiresAt = createdAt.plusMinutes(15);

    Booking booking = new Booking();
    booking.setUnit(testUnit);
    booking.setUser(testUser);
    booking.setStartDate(LocalDate.now().plusDays(1));
    booking.setEndDate(LocalDate.now().plusDays(5));
    booking.setStatus(BookingStatus.PENDING);
    booking.setCreatedAt(createdAt);
    booking.setExpiresAt(expiresAt);

    // When
    Booking saved = bookingRepository.save(booking);
    entityManager.flush();
    entityManager.clear();

    // Then
    Booking found = bookingRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedAt()).isEqualToIgnoringNanos(createdAt);
    assertThat(found.getExpiresAt()).isEqualToIgnoringNanos(expiresAt);
  }

  @Test
  @DisplayName("Should handle multiple bookings for same unit")
  void save_MultipleBookingsForSameUnit_ShouldWork() {
    // Given & When
    createBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), BookingStatus.PENDING);
    createBooking(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10), BookingStatus.PAID);
    createBooking(LocalDate.now().plusDays(11), LocalDate.now().plusDays(15),
        BookingStatus.CANCELLED);

    entityManager.flush();

    // Then
    List<Booking> allBookings = bookingRepository.findAll();
    assertThat(allBookings).hasSize(3);
    assertThat(allBookings)
        .extracting(Booking::getUnit)
        .extracting(Unit::getId)
        .containsOnly(testUnit.getId());
  }

  @Test
  @DisplayName("Should handle multiple bookings for same user")
  void save_MultipleBookingsForSameUser_ShouldWork() {
    // Given - create another unit
    Unit anotherUnit = Unit.builder()
        .numberOfRooms(3)
        .accommodationType(AccommodationType.APARTMENTS)
        .floor(5)
        .basePrice(new BigDecimal("150.00"))
        .markupPrice(new BigDecimal("172.50"))
        .description("Another unit")
        .createdBy(testUser)
        .build();
    anotherUnit = entityManager.persistAndFlush(anotherUnit);

    // When
    Booking booking1 = createBooking(LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5), BookingStatus.PENDING);

    Booking booking2 = new Booking();
    booking2.setUnit(anotherUnit);
    booking2.setUser(testUser);
    booking2.setStartDate(LocalDate.now().plusDays(1));
    booking2.setEndDate(LocalDate.now().plusDays(5));
    booking2.setStatus(BookingStatus.PENDING);
    booking2.setCreatedAt(LocalDateTime.now());
    booking2.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    booking2 = entityManager.persist(booking2);

    entityManager.flush();

    // Then
    List<Booking> allBookings = bookingRepository.findAll();
    assertThat(allBookings).hasSize(2);
    assertThat(allBookings)
        .extracting(Booking::getUser)
        .extracting(User::getId)
        .containsOnly(testUser.getId());
  }

  @Test
  @DisplayName("Should count bookings")
  void count_ShouldReturnCorrectCount() {
    // Given
    createBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), BookingStatus.PENDING);
    createBooking(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10), BookingStatus.PAID);
    createBooking(LocalDate.now().plusDays(11), LocalDate.now().plusDays(15),
        BookingStatus.CANCELLED);

    entityManager.flush();

    // When
    long count = bookingRepository.count();

    // Then
    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("Should check if booking exists by id")
  void existsById_ShouldReturnCorrectValue() {
    // Given
    Booking booking = createBooking(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5),
        BookingStatus.PENDING
    );
    entityManager.flush();

    // When & Then
    assertThat(bookingRepository.existsById(booking.getId())).isTrue();
    assertThat(bookingRepository.existsById(999L)).isFalse();
  }

  @Test
  @DisplayName("Should delete all bookings")
  void deleteAll_ShouldRemoveAllBookings() {
    // Given
    createBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), BookingStatus.PENDING);
    createBooking(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10), BookingStatus.PAID);
    entityManager.flush();

    // When
    bookingRepository.deleteAll();
    entityManager.flush();

    // Then
    assertThat(bookingRepository.count()).isZero();
  }

  // Helper method
  private Booking createBooking(LocalDate startDate, LocalDate endDate, BookingStatus status) {
    Booking booking = new Booking();
    booking.setUnit(testUnit);
    booking.setUser(testUser);
    booking.setStartDate(startDate);
    booking.setEndDate(endDate);
    booking.setStatus(status);
    booking.setCreatedAt(LocalDateTime.now());
    booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    return entityManager.persist(booking);
  }
}