package com.booking.web.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingCreateRequest(
    @NotNull(message = "Unit ID is required")
    Long unitId,

    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate
) {

}
