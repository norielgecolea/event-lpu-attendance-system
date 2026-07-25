package org.nors.dev.codes.lpu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nors.dev.codes.lpu.dto.AuthEventMessage;
import org.nors.dev.codes.lpu.dto.EventKioskStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Service
public class NotificationService {

    private static final Logger log = LogManager.getLogger(NotificationService.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_LIMIT = 512 * 1024;

    private final Map<String, WebSocketSession> portalSessions = new ConcurrentHashMap<>();
    /** Kiosk session id → event id string. */
    private final Map<String, String> eventKiosks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerPortal(WebSocketSession session, String username) {
        WebSocketSession safe = decorate(session);
        portalSessions.put(session.getId(), safe);
        log.info(
                "Portal WS registered: {} user={} (active={})",
                session.getId(),
                username,
                portalSessions.size()
        );
        sendTo(session, AuthEventMessage.of("AUTH_WS_CONNECTED", "Connected to live notifications"));
        sendTo(session, currentKioskPresence());
    }

    public void unregisterPortal(WebSocketSession session) {
        portalSessions.remove(session.getId());
        log.info("Portal WS unregistered: {} (active={})", session.getId(), portalSessions.size());
    }

    public void registerEventKiosk(WebSocketSession session, Long eventId) {
        eventKiosks.put(session.getId(), String.valueOf(eventId));
        log.info(
                "Event kiosk online: session={} eventId={} (kiosks={})",
                session.getId(),
                eventId,
                eventKiosks.size()
        );
        broadcast(currentKioskPresence());
    }

    public void unregisterEventKiosk(WebSocketSession session) {
        String removed = eventKiosks.remove(session.getId());
        log.info(
                "Event kiosk offline: session={} eventId={} (kiosks={})",
                session.getId(),
                removed,
                eventKiosks.size()
        );
        if (removed != null) {
            broadcast(currentKioskPresence());
        }
    }

    public EventKioskStatusResponse kioskStatus(Long eventId) {
        Map<String, Integer> counts = kioskCounts();
        List<String> activeIds = List.copyOf(new TreeSet<>(counts.keySet()));
        int count = eventId == null
                ? counts.values().stream().mapToInt(Integer::intValue).sum()
                : counts.getOrDefault(String.valueOf(eventId), 0);
        return new EventKioskStatusResponse(count > 0, count, activeIds, counts);
    }

    public void broadcastAttendanceTap(Object logPayload) {
        broadcast(AuthEventMessage.withPayload(
                "EVENT_ATTENDANCE_TAP",
                "Event attendance recorded",
                logPayload
        ));
    }

    public void broadcast(AuthEventMessage event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (IOException ex) {
            log.error("Failed to serialize WS event", ex);
            return;
        }
        broadcastRaw(payload);
    }

    private AuthEventMessage currentKioskPresence() {
        Map<String, Integer> counts = kioskCounts();
        List<String> eventIds = List.copyOf(new TreeSet<>(counts.keySet()));
        return AuthEventMessage.kioskPresence(eventIds, counts);
    }

    private Map<String, Integer> kioskCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String eventId : eventKiosks.values()) {
            counts.merge(eventId, 1, Integer::sum);
        }
        return counts;
    }

    private void broadcastRaw(String payload) {
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : portalSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                dead.add(entry.getKey());
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (Exception ex) {
                log.warn("Failed to send WS message to {}", entry.getKey(), ex);
                dead.add(entry.getKey());
            }
        }
        for (String id : dead) {
            portalSessions.remove(id);
        }
    }

    private void sendTo(WebSocketSession session, AuthEventMessage event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            WebSocketSession target = portalSessions.getOrDefault(session.getId(), session);
            if (target.isOpen()) {
                target.sendMessage(new TextMessage(payload));
            }
        } catch (Exception ex) {
            log.warn("Failed to send WS message to {}", session.getId(), ex);
        }
    }

    private static WebSocketSession decorate(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);
    }
}
