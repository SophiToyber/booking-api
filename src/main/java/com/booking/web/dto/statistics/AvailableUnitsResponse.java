package com.booking.web.dto.statistics;

import java.time.LocalDate;


public record AvailableUnitsResponse(
    long count,
    LocalDate startDate,
    LocalDate endDate,
    boolean fromCache
) {

  public static AvailableUnitsResponse of(long count, LocalDate startDate, LocalDate endDate) {
    return new AvailableUnitsResponse(count, startDate, endDate, false);
  }

  public AvailableUnitsResponse withCache(boolean cached) {
    return new AvailableUnitsResponse(count, startDate, endDate, cached);
  }
}
