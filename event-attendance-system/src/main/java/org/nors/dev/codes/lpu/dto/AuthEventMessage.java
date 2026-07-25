package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AuthEventMessage(
        String type,
        String message,
        String username,
        Instant timestamp,
        Object payload,
        List<String> eventIds,
        Map<String, Integer> kioskCounts
) {
    public static AuthEventMessage of(String type, String message) {
        return new AuthEventMessage(type, message, null, Instant.now(), null, null, null);
    }

    public static AuthEventMessage withPayload(String type, String message, Object payload) {
        return new AuthEventMessage(type, message, null, Instant.now(), payload, null, null);
    }

    public static AuthEventMessage kioskPresence(List<String> eventIds, Map<String, Integer> counts) {
        return new AuthEventMessage(
                "EVENT_KIOSK_PRESENCE",
                "Event kiosk presence updated",
                null,
                Instant.now(),
                null,
                eventIds,
                counts
        );
    }
}
