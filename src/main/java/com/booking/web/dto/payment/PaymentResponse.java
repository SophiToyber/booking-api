package com.booking.web.dto.payment;

import com.booking.entity.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment response")
public record PaymentResponse(
    @Schema(description = "Payment ID", example = "1")
    Long id,

    @Schema(description = "Booking ID", example = "1")
    Long bookingId,

    @Schema(description = "Payment amount", example = "460.00")
    BigDecimal amount,

    @Schema(description = "Payment status")
    PaymentStatus status,

    @Schema(description = "Payment date and time")
    LocalDateTime paidAt,

    @Schema(description = "Creation date and time")
    LocalDateTime createdAt
) {

}