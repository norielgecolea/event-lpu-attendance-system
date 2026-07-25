package org.nors.dev.codes.lpu.websocket;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.nors.dev.codes.lpu.repository.EventRepository;
import org.nors.dev.codes.lpu.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Public kiosk presence channel — one connection per open /attend/:id page. */
@Component
public class EventKioskWebSocketHandler extends TextWebSocketHandler {

    private final NotificationService notificationService;
    private final EventRepository eventRepository;

    public EventKioskWebSocketHandler(
            NotificationService notificationService,
            EventRepository eventRepository
    ) {
        this.notificationService = notificationService;
        this.eventRepository = eventRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long eventId = extractEventId(session);
        if (eventId == null || eventRepository.findActiveById(eventId).isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid event"));
            return;
        }
        notificationService.registerEventKiosk(session, eventId);
        session.sendMessage(new TextMessage(
                "{\"type\":\"EVENT_KIOSK_CONNECTED\",\"message\":\"Kiosk registered\",\"eventIds\":[\""
                        + eventId
                        + "\"]}"
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notificationService.unregisterEventKiosk(session);
    }

    private Long extractEventId(WebSocketSession session) {
        Object attr = session.getAttributes().get("eventId");
        if (attr instanceof Long id) {
            return id;
        }
        if (attr instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        for (String part : session.getUri().getQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "eventId".equals(kv[0])) {
                try {
                    return Long.parseLong(URLDecoder.decode(kv[1], StandardCharsets.UTF_8).trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
