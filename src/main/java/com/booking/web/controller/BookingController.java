package com.booking.web.controller;

import com.booking.service.BookingService;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.booking.web.dto.booking.BookingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse create(@RequestBody @Valid BookingCreateRequest req) {
    return bookingService.create(req);
  }

  @GetMapping("/{id}")
  public BookingResponse get(@PathVariable Long id) {
    return bookingService.getById(id);
  }

  @PutMapping("/{id}/cancel")
  public BookingResponse cancel(@PathVariable Long id) {
    return bookingService.cancel(id);
  }

  @PutMapping("/{id}/pay")
  public BookingResponse pay(@PathVariable Long id) {
    return bookingService.pay(id);
  }
}