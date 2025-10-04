package com.booking.web.controller;

import com.booking.service.UnitService;
import com.booking.web.dto.unit.UnitCreateRequest;
import com.booking.web.dto.unit.UnitResponse;
import com.booking.web.dto.unit.UnitSearchRequest;
import com.booking.web.dto.unit.UnitUpdateRequest;
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
public class UnitController {

  private final UnitService unitService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UnitResponse create(@RequestBody @Valid UnitCreateRequest req) {
    return unitService.create(req);
  }

  @GetMapping("/{id}")
  public UnitResponse get(@PathVariable Long id) {
    return unitService.getById(id);
  }

  @GetMapping
  public Page<UnitResponse> list(Pageable pageable) {
    return unitService.list(pageable);
  }
  
  @PostMapping("/search")
  public Page<UnitResponse> searchAvailable(
      @RequestBody @Valid UnitSearchRequest req,
      Pageable pageable) {
    return unitService.searchAvailable(req, pageable);
  }

  @PutMapping("/{id}")
  public UnitResponse update(@PathVariable Long id, @RequestBody @Valid UnitUpdateRequest req) {
    return unitService.update(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    unitService.delete(id);
  }
}