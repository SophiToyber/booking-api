package com.booking.service;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.mapper.UnitMapper;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.statistics.AvailableUnitsResponse;
import com.booking.web.dto.unit.UnitCreateRequest;
import com.booking.web.dto.unit.UnitResponse;
import com.booking.web.dto.unit.UnitSearchRequest;
import com.booking.web.dto.unit.UnitUpdateRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UnitService {

  private final UnitRepository unitRepository;
  private final UserRepository userRepository;
  private final UnitMapper unitMapper;

  public UnitResponse create(UnitCreateRequest req) {
    User ownerRef = userRepository.getReferenceById(req.createdById());

    Unit unit = Unit.builder()
        .numberOfRooms(req.numberOfRooms())
        .accommodationType(req.accommodationType())
        .floor(req.floor())
        .basePrice(req.basePrice())
        .markupPrice(applyMarkup(req.basePrice()))
        .description(req.description())
        .createdBy(ownerRef)
        .build();

    Unit saved = unitRepository.save(unit);
    return unitMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public UnitResponse getById(Long id) {
    Unit unit = unitRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Unit %d not found".formatted(id)));
    return unitMapper.toDto(unit);
  }

  @Transactional(readOnly = true)
  public Page<UnitResponse> list(Pageable pageable) {
    return unitRepository.findAll(pageable).map(unitMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<UnitResponse> searchAvailable(UnitSearchRequest req, Pageable pageable) {
    if (req.endDate().isBefore(req.startDate()) || req.endDate().isEqual(req.startDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "End date must be after start date");
    }

    Page<Unit> units = unitRepository.findAvailableUnits(
        req.numberOfRooms(),
        req.accommodationType(),
        req.floor(),
        req.minPrice(),
        req.maxPrice(),
        req.startDate(),
        req.endDate(),
        pageable
    );

    return units.map(unitMapper::toDto);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "availableUnitsCount")
  public AvailableUnitsResponse getAvailableCount(LocalDate startDate, LocalDate endDate) {
    if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "End date must be after start date");
    }

    log.debug("Calculating available units count for period {} to {} (cache miss)",
        startDate, endDate);

    long count = unitRepository.countAvailableUnits(startDate, endDate);

    return AvailableUnitsResponse.of(count, startDate, endDate);
  }

  public UnitResponse update(Long id, UnitUpdateRequest req) {
    Unit unit = unitRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Unit %d not found".formatted(id)));

    unit.setNumberOfRooms(req.numberOfRooms());
    unit.setAccommodationType(req.accommodationType());
    unit.setFloor(req.floor());
    unit.setBasePrice(req.basePrice());
    unit.setMarkupPrice(applyMarkup(req.basePrice()));
    unit.setDescription(req.description());

    return unitMapper.toDto(unit);
  }

  public void delete(Long id) {
    Unit unit = unitRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Unit %d not found".formatted(id)));

    unitRepository.delete(unit);
  }

  private BigDecimal applyMarkup(BigDecimal base) {
    return base.multiply(new BigDecimal("1.15")).setScale(2, RoundingMode.HALF_UP);
  }
}