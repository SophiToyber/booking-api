package com.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.entity.Event;
import com.booking.entity.enums.EventType;
import com.booking.mapper.EventMapper;
import com.booking.repository.EventRepository;
import com.booking.web.dto.event.EventResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

  @Mock
  private EventRepository eventRepository;

  @Mock
  private EventMapper eventMapper;

  @InjectMocks
  private EventService eventService;

  private Event mockEvent;
  private EventResponse mockResponse;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();

    mockEvent = Event.builder()
        .id(1L)
        .eventType(EventType.BOOKING_CREATED)
        .entityType("Booking")
        .entityId(1L)
        .details("Booking created by user 5")
        .createdAt(now)
        .build();

    mockResponse = new EventResponse(
        1L,
        EventType.BOOKING_CREATED,
        "Booking",
        1L,
        "Booking created by user 5",
        now
    );
  }

  @Test
  @DisplayName("Should log event successfully")
  void logEvent_ValidData_ShouldSaveEvent() {
    // Given
    when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

    // When
    eventService.logEvent(
        EventType.BOOKING_CREATED,
        "Booking",
        1L,
        "Booking created by user 5"
    );

    // Then
    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventCaptor.capture());

    Event savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEventType()).isEqualTo(EventType.BOOKING_CREATED);
    assertThat(savedEvent.getEntityType()).isEqualTo("Booking");
    assertThat(savedEvent.getEntityId()).isEqualTo(1L);
    assertThat(savedEvent.getDetails()).isEqualTo("Booking created by user 5");
    assertThat(savedEvent.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should log different event types")
  void logEvent_DifferentTypes_ShouldSaveAll() {
    // Given
    when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

    // When - PAYMENT_COMPLETED
    eventService.logEvent(EventType.PAYMENT_COMPLETED, "Payment", 1L, "Payment completed");

    // BOOKING_CANCELLED
    eventService.logEvent(EventType.BOOKING_CANCELLED, "Booking", 2L, "Booking cancelled");

    // BOOKING_EXPIRED
    eventService.logEvent(EventType.BOOKING_EXPIRED, "Booking", 3L, "Booking expired");

    // Then - Verify save called 3 times (changed from 1)
    verify(eventRepository, times(3)).save(any(Event.class));
  }

  @Test
  @DisplayName("Should get events by entity with pagination")
  void getEventsByEntity_ValidEntity_ShouldReturnPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<Event> events = List.of(mockEvent);  // Changed to List

    when(eventRepository.findByEntityTypeAndEntityId("Booking", 1L))
        .thenReturn(events);  // Mock the correct method
    when(eventMapper.toDto(mockEvent)).thenReturn(mockResponse);

    // When
    Page<EventResponse> result = eventService.getEventsByEntity("Booking", 1L, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).entityType()).isEqualTo("Booking");
    assertThat(result.getContent().get(0).entityId()).isEqualTo(1L);

    // Verify the correct method was called
    verify(eventRepository).findByEntityTypeAndEntityId("Booking", 1L);
  }

  @Test
  @DisplayName("Should get events by type with pagination")
  void getEventsByType_ValidType_ShouldReturnPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> eventPage = new PageImpl<>(List.of(mockEvent));

    when(eventRepository.findByEventType(EventType.BOOKING_CREATED, pageable))
        .thenReturn(eventPage);
    when(eventMapper.toDto(mockEvent)).thenReturn(mockResponse);

    // When
    Page<EventResponse> result = eventService.getEventsByType(EventType.BOOKING_CREATED, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).eventType()).isEqualTo(EventType.BOOKING_CREATED);
    verify(eventRepository).findByEventType(EventType.BOOKING_CREATED, pageable);
  }

  @Test
  @DisplayName("Should get events by date range with pagination")
  void getEventsByDateRange_ValidRange_ShouldReturnPage() {
    // Given
    LocalDateTime start = LocalDateTime.of(2025, 10, 1, 0, 0);
    LocalDateTime end = LocalDateTime.of(2025, 10, 31, 23, 59);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> eventPage = new PageImpl<>(List.of(mockEvent));

    when(eventRepository.findByCreatedAtBetween(start, end, pageable))
        .thenReturn(eventPage);
    when(eventMapper.toDto(mockEvent)).thenReturn(mockResponse);

    // When
    Page<EventResponse> result = eventService.getEventsByDateRange(start, end, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(eventRepository).findByCreatedAtBetween(start, end, pageable);
  }

  @Test
  @DisplayName("Should get event by ID")
  void getById_ExistingEvent_ShouldReturnEvent() {
    // Given
    when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
    when(eventMapper.toDto(mockEvent)).thenReturn(mockResponse);

    // When
    EventResponse result = eventService.getById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.eventType()).isEqualTo(EventType.BOOKING_CREATED);
    verify(eventRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when event not found")
  void getById_NotFound_ShouldThrowException() {
    // Given
    when(eventRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> eventService.getById(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Event 999 not found");
  }

  @Test
  @DisplayName("Should handle empty results for events by type")
  void getEventsByType_NoEvents_ShouldReturnEmptyPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> emptyPage = new PageImpl<>(List.of());

    when(eventRepository.findByEventType(EventType.PAYMENT_FAILED, pageable))
        .thenReturn(emptyPage);

    // When
    Page<EventResponse> result = eventService.getEventsByType(EventType.PAYMENT_FAILED, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();
  }

  @Test
  @DisplayName("Should handle empty results for date range")
  void getEventsByDateRange_NoEvents_ShouldReturnEmptyPage() {
    // Given
    LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> emptyPage = new PageImpl<>(List.of());

    when(eventRepository.findByCreatedAtBetween(start, end, pageable))
        .thenReturn(emptyPage);

    // When
    Page<EventResponse> result = eventService.getEventsByDateRange(start, end, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("Should log event with null details")
  void logEvent_NullDetails_ShouldSaveEvent() {
    // Given
    when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

    // When
    eventService.logEvent(EventType.BOOKING_CANCELLED, "Booking", 1L, null);

    // Then
    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(captor.capture());
    assertThat(captor.getValue().getDetails()).isNull();
  }

  @Test
  @DisplayName("Should log events for all event types")
  void logEvent_AllEventTypes_ShouldSaveCorrectly() {
    // Given
    when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

    // When - log each event type
    eventService.logEvent(EventType.UNIT_CREATED, "Unit", 1L, "Unit created");
    eventService.logEvent(EventType.UNIT_UPDATED, "Unit", 1L, "Unit updated");
    eventService.logEvent(EventType.UNIT_DELETED, "Unit", 1L, "Unit deleted");
    eventService.logEvent(EventType.BOOKING_CREATED, "Booking", 1L, "Booking created");
    eventService.logEvent(EventType.BOOKING_CANCELLED, "Booking", 1L, "Booking cancelled");
    eventService.logEvent(EventType.BOOKING_EXPIRED, "Booking", 1L, "Booking expired");
    eventService.logEvent(EventType.PAYMENT_COMPLETED, "Payment", 1L, "Payment completed");
    eventService.logEvent(EventType.PAYMENT_FAILED, "Payment", 1L, "Payment failed");
    eventService.logEvent(EventType.CACHE_INVALIDATED, "Cache", 1L, "Cache invalidated");

    // Then - verify save called 9 times (once for each event type)
    verify(eventRepository, times(9)).save(any(Event.class));
  }
}