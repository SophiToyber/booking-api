package com.booking.web.dto.booking;

import com.booking.entity.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    Long unitId,
    Long userId,
    LocalDate startDate,
    LocalDate endDate,
    BookingStatus status,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {

}