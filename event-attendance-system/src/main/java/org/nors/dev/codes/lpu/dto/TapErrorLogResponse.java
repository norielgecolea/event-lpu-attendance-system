package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import org.nors.dev.codes.lpu.model.TapErrorLog;

public record TapErrorLogResponse(
        String id,
        String identifier,
        String eventId,
        String eventTitle,
        String location,
        Instant tappedAt
) {
    public static TapErrorLogResponse from(TapErrorLog log) {
        return new TapErrorLogResponse(
                String.valueOf(log.getId()),
                log.getIdentifier(),
                log.getEventId() == null ? null : String.valueOf(log.getEventId()),
                log.getEventTitle(),
                log.getLocation(),
                log.getTappedAt()
        );
    }
}
