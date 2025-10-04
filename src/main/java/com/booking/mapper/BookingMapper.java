package com.booking.mapper;

import com.booking.entity.Booking;
import com.booking.web.dto.booking.BookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

  @Mapping(source = "unit.id", target = "unitId")
  @Mapping(source = "user.id", target = "userId")
  BookingResponse toDto(Booking booking);
}
