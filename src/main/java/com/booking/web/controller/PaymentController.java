package com.booking.web.controller;

import com.booking.service.PaymentService;
import com.booking.web.dto.payment.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/booking/{bookingId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Process payment for booking",
      description = "Processes payment for a pending booking. Booking must not be expired or cancelled."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment completed successfully"),
      @ApiResponse(responseCode = "400", description = "Booking expired, cancelled or already paid"),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  public PaymentResponse processPayment(
      @Parameter(description = "Booking ID", example = "1")
      @PathVariable Long bookingId) {
    return paymentService.processPayment(bookingId);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get payment by ID",
      description = "Retrieves payment information by payment ID"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found"),
      @ApiResponse(responseCode = "404", description = "Payment not found")
  })
  public PaymentResponse getById(
      @Parameter(description = "Payment ID", example = "1")
      @PathVariable Long id) {
    return paymentService.getById(id);
  }

  @GetMapping("/booking/{bookingId}")
  @Operation(
      summary = "Get payment by booking ID",
      description = "Retrieves payment information for a specific booking"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found"),
      @ApiResponse(responseCode = "404", description = "Payment not found")
  })
  public PaymentResponse getByBookingId(
      @Parameter(description = "Booking ID", example = "1")
      @PathVariable Long bookingId) {
    return paymentService.getByBookingId(bookingId);
  }
}