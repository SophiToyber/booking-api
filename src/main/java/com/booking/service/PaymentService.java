package com.booking.service;

import com.booking.entity.Booking;
import com.booking.entity.Payment;
import com.booking.entity.enums.BookingStatus;
import com.booking.entity.enums.EventType;
import com.booking.entity.enums.PaymentStatus;
import com.booking.mapper.PaymentMapper;
import com.booking.repository.BookingRepository;
import com.booking.repository.PaymentRepository;
import com.booking.web.dto.payment.PaymentResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final BookingRepository bookingRepository;
  private final PaymentMapper paymentMapper;
  private final EventService eventService;

  @CacheEvict(value = "availableUnitsCount", allEntries = true)
  public PaymentResponse processPayment(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Booking %d not found".formatted(bookingId)));

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
      log.info("Booking {} expired, automatically cancelled. Cache invalidated.", bookingId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Booking has expired and was cancelled");
    }

    BigDecimal amount = calculateTotalAmount(booking);

    Payment payment = Payment.builder()
        .booking(booking)
        .amount(amount)
        .status(PaymentStatus.COMPLETED)
        .paidAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .build();

    Payment saved = paymentRepository.save(payment);

    booking.setStatus(BookingStatus.PAID);

    eventService.logEvent(EventType.PAYMENT_COMPLETED, "Payment", saved.getId(),
        "Payment completed for booking " + bookingId + ", amount: " + amount);

    log.info("Payment {} completed for booking {}. Amount: {}. Cache invalidated.",
        saved.getId(), bookingId, amount);

    return paymentMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public PaymentResponse getByBookingId(Long bookingId) {
    Payment payment = paymentRepository.findByBookingId(bookingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Payment for booking %d not found".formatted(bookingId)));
    return paymentMapper.toDto(payment);
  }

  @Transactional(readOnly = true)
  public PaymentResponse getById(Long id) {
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Payment %d not found".formatted(id)));
    return paymentMapper.toDto(payment);
  }

  private BigDecimal calculateTotalAmount(Booking booking) {
    long nights = ChronoUnit.DAYS.between(
        booking.getStartDate(),
        booking.getEndDate()
    );
    return booking.getUnit().getMarkupPrice()
        .multiply(BigDecimal.valueOf(nights));
  }
}