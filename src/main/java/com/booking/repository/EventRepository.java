package com.booking.repository;

import com.booking.entity.Event;
import com.booking.entity.enums.EventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

  List<Event> findByEntityTypeAndEntityId(String entityType, Long entityId);

  Page<Event> findByEventType(EventType eventType, Pageable pageable);

  Page<Event> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}