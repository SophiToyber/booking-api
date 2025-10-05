package com.booking.web.controller;

import com.booking.service.UnitService;
import com.booking.web.dto.unit.UnitCreateRequest;
import com.booking.web.dto.unit.UnitResponse;
import com.booking.web.dto.unit.UnitSearchRequest;
import com.booking.web.dto.unit.UnitUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@Tag(name = "Units", description = "Accommodation unit management endpoints")
public class UnitController {

  private final UnitService unitService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a new unit",
      description = "Creates a new accommodation unit with 15% markup automatically applied to base price"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Unit created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "404", description = "User (owner) not found")
  })
  public UnitResponse create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Unit creation request",
          required = true
      )
      @RequestBody @Valid UnitCreateRequest req) {
    return unitService.create(req);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get unit by ID",
      description = "Retrieves unit information by unit ID"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Unit found"),
      @ApiResponse(responseCode = "404", description = "Unit not found")
  })
  public UnitResponse get(
      @Parameter(description = "Unit ID", example = "1")
      @PathVariable Long id) {
    return unitService.getById(id);
  }

  @GetMapping
  @Operation(
      summary = "List all units",
      description = "Retrieves a paginated list of all units"
  )
  @ApiResponse(responseCode = "200", description = "Units retrieved successfully")
  public Page<UnitResponse> list(
      @Parameter(description = "Pagination parameters")
      Pageable pageable) {
    return unitService.list(pageable);
  }

  @PostMapping("/search")
  @Operation(
      summary = "Search available units",
      description = "Searches for units available in a specific date range with optional filters (rooms, type, floor, price)"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Search completed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid date range")
  })
  public Page<UnitResponse> searchAvailable(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Search criteria with date range (required) and optional filters",
          required = true
      )
      @RequestBody @Valid UnitSearchRequest req,

      @Parameter(description = "Pagination and sorting parameters")
      Pageable pageable) {
    return unitService.searchAvailable(req, pageable);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update unit",
      description = "Updates an existing unit. Markup price is recalculated automatically."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Unit updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "404", description = "Unit not found")
  })
  public UnitResponse update(
      @Parameter(description = "Unit ID", example = "1")
      @PathVariable Long id,

      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Unit update request",
          required = true
      )
      @RequestBody @Valid UnitUpdateRequest req) {
    return unitService.update(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete unit",
      description = "Deletes a unit by ID"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Unit deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Unit not found")
  })
  public void delete(
      @Parameter(description = "Unit ID", example = "1")
      @PathVariable Long id) {
    unitService.delete(id);
  }
}