package com.booking.web.controller;

import com.booking.service.UnitService;
import com.booking.web.dto.statistics.AvailableUnitsResponse;
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
public class StatisticsController {

  private final UnitService unitService;
  
  @GetMapping("/available-units")
  public AvailableUnitsResponse getAvailableUnitsCount(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return unitService.getAvailableCount(startDate, endDate);
  }
}