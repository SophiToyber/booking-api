package com.booking.web.dto.unit;

import com.booking.entity.enums.AccommodationType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UnitSearchRequest(
    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    Integer numberOfRooms,
    AccommodationType accommodationType,
    Integer floor,
    BigDecimal minPrice,
    BigDecimal maxPrice
) {

}
