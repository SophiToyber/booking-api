package com.booking.web.dto.unit;

import com.booking.entity.enums.AccommodationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UnitCreateRequest(
    @NotNull @Positive
    Integer numberOfRooms,

    @NotNull
    AccommodationType accommodationType,

    @NotNull
    @Min(0)
    Integer floor,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be > 0")
    BigDecimal basePrice,

    @NotBlank
    String description,

    @NotNull
    Long createdById
) {

}
