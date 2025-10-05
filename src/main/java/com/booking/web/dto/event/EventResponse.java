package com.booking.web.dto.event;

import com.booking.entity.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Event response")
public record EventResponse(
    @Schema(description = "Event ID", example = "1")
    Long id,

    @Schema(description = "Event type")
    EventType eventType,

    @Schema(description = "Entity type", example = "Booking")
    String entityType,

    @Schema(description = "Entity ID", example = "1")
    Long entityId,

    @Schema(description = "Event details", example = "Booking created by user 5")
    String details,

    @Schema(description = "Event creation timestamp")
    LocalDateTime createdAt
) {

}