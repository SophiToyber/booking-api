package com.booking.mapper;

import com.booking.entity.Unit;
import com.booking.web.dto.unit.UnitResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UnitMapper {

  @Mapping(source = "createdBy.id", target = "createdById")
  UnitResponse toDto(Unit unit);
}
