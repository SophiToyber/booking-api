package com.booking.web.dto.statistics;

import java.time.LocalDate;

public record AvailableUnitsResponse(
    long count,
    LocalDate startDate,
    LocalDate endDate
) {

  public static AvailableUnitsResponse of(long count, LocalDate startDate, LocalDate endDate) {
    return new AvailableUnitsResponse(count, startDate, endDate);
  }
}
