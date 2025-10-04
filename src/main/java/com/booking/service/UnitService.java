package com.booking.service;

import com.booking.entity.Unit;
import com.booking.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnitService {

  private final UnitRepository unitRepository;

  public Unit getUnitById(Long id) {
    return unitRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Unit not found, id: " + id));
  }

  public Unit saveUnit(Unit unit) {
    return unitRepository.save(unit);
  }

}
