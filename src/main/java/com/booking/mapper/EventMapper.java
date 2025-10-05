package com.booking.mapper;

import com.booking.entity.Event;
import com.booking.web.dto.event.EventResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

  EventResponse toDto(Event event);
}