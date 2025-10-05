package com.booking.web.controller;

import com.booking.service.UnitService;
import com.booking.web.dto.statistics.AvailableUnitsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics and analytics endpoints")
public class StatisticsController {

  private final UnitService unitService;

  @GetMapping("/available-units")
  @Operation(
      summary = "Get available units count",
      description = "Returns the number of units available for booking in the specified date range. Result is cached for performance."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid date range (end date must be after start date)")
  })
  public AvailableUnitsResponse getAvailableUnitsCount(
      @Parameter(
          description = "Start date of the booking period",
          example = "2025-11-01",
          required = true
      )
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

      @Parameter(
          description = "End date of the booking period",
          example = "2025-11-05",
          required = true
      )
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return unitService.getAvailableCount(startDate, endDate);
  }
}