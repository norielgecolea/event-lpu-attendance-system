package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import org.nors.dev.codes.lpu.model.Event;

public record EventResponse(
        String id,
        String title,
        String description,
        String location,
        String photo,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        String createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                String.valueOf(event.getId()),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getPhoto(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.isActive(),
                event.getCreatedByUserId() == null ? null : String.valueOf(event.getCreatedByUserId()),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
