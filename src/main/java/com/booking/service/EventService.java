package com.booking.service;

import com.booking.entity.Event;
import com.booking.entity.enums.EventType;
import com.booking.mapper.EventMapper;
import com.booking.repository.EventRepository;
import com.booking.web.dto.event.EventResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRepository eventRepository;
  private final EventMapper eventMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logEvent(EventType eventType, String entityType, Long entityId, String details) {
    Event event = Event.builder()
        .eventType(eventType)
        .entityType(entityType)
        .entityId(entityId)
        .details(details)
        .createdAt(LocalDateTime.now())
        .build();

    eventRepository.save(event);
    log.debug("Event logged: {} for {}:{} - {}", eventType, entityType, entityId, details);
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> getEventsByEntity(String entityType, Long entityId,
      Pageable pageable) {
    List<Event> events = eventRepository.findByEntityTypeAndEntityId(entityType, entityId);

    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), events.size());

    List<Event> pageContent = events.subList(start, end);
    Page<Event> eventPage = new PageImpl<>(pageContent, pageable, events.size());

    return eventPage.map(eventMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> getEventsByType(EventType eventType, Pageable pageable) {
    return eventRepository.findByEventType(eventType, pageable).map(eventMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> getEventsByDateRange(LocalDateTime start, LocalDateTime end,
      Pageable pageable) {
    return eventRepository.findByCreatedAtBetween(start, end, pageable).map(eventMapper::toDto);
  }

  @Transactional(readOnly = true)
  public EventResponse getById(Long id) {
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Event %d not found".formatted(id)));
    return eventMapper.toDto(event);
  }
}