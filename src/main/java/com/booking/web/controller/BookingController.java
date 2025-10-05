package com.booking.web.controller;

import com.booking.service.BookingService;
import com.booking.web.dto.booking.BookingCreateRequest;
import com.booking.web.dto.booking.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

  private final BookingService bookingService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a new booking",
      description = "Creates a new booking for a unit. Unit must be available for the selected dates. Booking expires in 15 minutes if not paid."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Booking created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request or unit not available"),
      @ApiResponse(responseCode = "404", description = "Unit or user not found"),
      @ApiResponse(responseCode = "409", description = "Unit is not available for selected dates")
  })
  public BookingResponse create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Booking creation request",
          required = true
      )
      @RequestBody @Valid BookingCreateRequest req) {
    return bookingService.create(req);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get booking by ID",
      description = "Retrieves booking information by booking ID"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Booking found"),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  public BookingResponse get(
      @Parameter(description = "Booking ID", example = "1")
      @PathVariable Long id) {
    return bookingService.getById(id);
  }

  @PutMapping("/{id}/cancel")
  @Operation(
      summary = "Cancel booking",
      description = "Cancels a pending booking. Cannot cancel paid or already cancelled bookings."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot cancel paid or already cancelled booking"),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  public BookingResponse cancel(
      @Parameter(description = "Booking ID", example = "1")
      @PathVariable Long id) {
    return bookingService.cancel(id);
  }

  @PutMapping("/{id}/pay")
  @Operation(
      summary = "Pay for booking (deprecated - use /api/payments instead)",
      description = "Marks booking as paid. This endpoint is deprecated, use POST /api/payments/booking/{bookingId} instead."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Booking paid successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot pay for cancelled, expired or already paid booking"),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  @Deprecated
  public BookingResponse pay(
      @Parameter(description = "Booking ID", example = "1")
      @PathVariable Long id) {
    return bookingService.pay(id);
  }
}