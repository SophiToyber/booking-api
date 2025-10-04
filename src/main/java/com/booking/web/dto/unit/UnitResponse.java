package com.booking.web.dto.unit;

import com.booking.entity.enums.AccommodationType;
import java.math.BigDecimal;

public record UnitResponse(
    Long id,
    Integer numberOfRooms,
    AccommodationType accommodationType,
    Integer floor,
    String description,
    BigDecimal basePrice,
    BigDecimal markupPrice,
    Long createdById
) {

}