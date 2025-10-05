package com.booking.web.controller;

import com.booking.entity.enums.EventType;
import com.booking.service.EventService;
import com.booking.web.dto.event.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Audit events endpoints")
public class EventController {

  private final EventService eventService;

  @GetMapping("/{id}")
  @Operation(
      summary = "Get event by ID",
      description = "Retrieves a specific event by its ID"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Event found"),
      @ApiResponse(responseCode = "404", description = "Event not found")
  })
  public EventResponse getById(
      @Parameter(description = "Event ID", example = "1")
      @PathVariable Long id) {
    return eventService.getById(id);
  }

  @GetMapping
  @Operation(
      summary = "Get events by entity",
      description = "Retrieves events for a specific entity type and ID with pagination"
  )
  @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  public Page<EventResponse> getEvents(
      @Parameter(description = "Entity type", example = "Booking")
      @RequestParam String entityType,

      @Parameter(description = "Entity ID", example = "1")
      @RequestParam Long entityId,

      Pageable pageable) {
    return eventService.getEventsByEntity(entityType, entityId, pageable);
  }

  @GetMapping("/type/{eventType}")
  @Operation(
      summary = "Get events by type",
      description = "Retrieves all events of a specific type with pagination"
  )
  @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  public Page<EventResponse> getEventsByType(
      @Parameter(description = "Event type")
      @PathVariable EventType eventType,

      Pageable pageable) {
    return eventService.getEventsByType(eventType, pageable);
  }

  @GetMapping("/date-range")
  @Operation(
      summary = "Get events by date range",
      description = "Retrieves events within a specific date range with pagination"
  )
  @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  public Page<EventResponse> getEventsByDateRange(
      @Parameter(description = "Start date and time")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,

      @Parameter(description = "End date and time")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,

      Pageable pageable) {
    return eventService.getEventsByDateRange(start, end, pageable);
  }
}