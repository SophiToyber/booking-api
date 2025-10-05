package com.booking.service;

import com.booking.entity.Booking;
import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.BookingStatus;
import com.booking.entity.enums.EventType;
import com.booking.mapper.BookingMapper;
import com.booking.repository.BookingRepository;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.booking.web.dto.booking.BookingResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

  private final BookingRepository bookingRepository;
  private final UnitRepository unitRepository;
  private final UserRepository userRepository;
  private final BookingMapper bookingMapper;
  private final EventService eventService;

  @CacheEvict(value = "availableUnitsCount", allEntries = true)
  public BookingResponse create(BookingCreateRequest req) {
    if (req.endDate().isBefore(req.startDate()) || req.endDate().isEqual(req.startDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "End date must be after start date");
    }

    Unit unit = unitRepository.findById(req.unitId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Unit %d not found".formatted(req.unitId())));

    User user = userRepository.findById(req.userId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User %d not found".formatted(req.userId())));

    boolean isAvailable = checkUnitAvailability(req.unitId(), req.startDate(), req.endDate());
    if (!isAvailable) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Unit is not available for the selected dates");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiresAt = now.plusMinutes(15);

    Booking booking = new Booking();
    booking.setUnit(unit);
    booking.setUser(user);
    booking.setStartDate(req.startDate());
    booking.setEndDate(req.endDate());
    booking.setStatus(BookingStatus.PENDING);
    booking.setCreatedAt(now);
    booking.setExpiresAt(expiresAt);

    Booking saved = bookingRepository.save(booking);

    eventService.logEvent(EventType.BOOKING_CREATED, "Booking", saved.getId(),
        "Booking created for unit " + unit.getId() + " by user " + user.getId());

    log.info("Created booking {} for unit {} by user {}, expires at {}. Cache invalidated.",
        saved.getId(), unit.getId(), user.getId(), expiresAt);

    return bookingMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public BookingResponse getById(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Booking %d not found".formatted(id)));
    return bookingMapper.toDto(booking);
  }

  @CacheEvict(value = "availableUnitsCount", allEntries = true)
  public BookingResponse cancel(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Booking %d not found".formatted(id)));

    if (booking.getStatus() == BookingStatus.CANCELLED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Booking is already cancelled");
    }

    if (booking.getStatus() == BookingStatus.PAID) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Cannot cancel paid booking");
    }

    booking.setStatus(BookingStatus.CANCELLED);

    eventService.logEvent(EventType.BOOKING_CANCELLED, "Booking", booking.getId(),
        "Booking cancelled by user");

    log.info("Cancelled booking {}. Cache invalidated.", id);

    return bookingMapper.toDto(booking);
  }

  @CacheEvict(value = "availableUnitsCount", allEntries = true)
  public BookingResponse pay(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Booking %d not found".formatted(id)));

    if (booking.getStatus() == BookingStatus.CANCELLED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Cannot pay for cancelled booking");
    }

    if (booking.getStatus() == BookingStatus.PAID) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Booking is already paid");
    }

    if (LocalDateTime.now().isAfter(booking.getExpiresAt())) {
      booking.setStatus(BookingStatus.CANCELLED);
      eventService.logEvent(EventType.BOOKING_EXPIRED, "Booking", booking.getId(),
          "Booking expired before payment");
      log.info("Booking {} expired, automatically cancelled. Cache invalidated.", id);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Booking has expired and was cancelled");
    }

    booking.setStatus(BookingStatus.PAID);

    eventService.logEvent(EventType.PAYMENT_COMPLETED, "Booking", booking.getId(),
        "Payment completed via deprecated booking/pay endpoint");

    log.info("Paid booking {}. Cache invalidated.", id);

    return bookingMapper.toDto(booking);
  }

  private boolean checkUnitAvailability(Long unitId,
      java.time.LocalDate startDate,
      java.time.LocalDate endDate) {
    var existingBookings = bookingRepository.findAll().stream()
        .filter(b -> b.getUnit().getId().equals(unitId))
        .filter(b -> b.getStatus() == BookingStatus.PENDING
            || b.getStatus() == BookingStatus.PAID)
        .toList();

    for (Booking existing : existingBookings) {
      boolean overlaps = !startDate.isAfter(existing.getEndDate())
          && !endDate.isBefore(existing.getStartDate());

      if (overlaps) {
        log.debug("Unit {} is not available: overlaps with booking {}",
            unitId, existing.getId());
        return false;
      }
    }

    return true;
  }

  @CacheEvict(value = "availableUnitsCount", allEntries = true)
  public void cancelExpiredBookings() {
    LocalDateTime now = LocalDateTime.now();

    var expiredBookings = bookingRepository.findAll().stream()
        .filter(b -> b.getStatus() == BookingStatus.PENDING)
        .filter(b -> b.getExpiresAt().isBefore(now))
        .toList();

    for (Booking booking : expiredBookings) {
      booking.setStatus(BookingStatus.CANCELLED);

      eventService.logEvent(EventType.BOOKING_EXPIRED, "Booking", booking.getId(),
          "Booking automatically cancelled due to expiration");

      log.info("Auto-cancelled expired booking {}", booking.getId());
    }

    if (!expiredBookings.isEmpty()) {
      log.info("Auto-cancelled {} expired bookings. Cache invalidated.", expiredBookings.size());
    }
  }
}