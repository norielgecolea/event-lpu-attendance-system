package org.nors.dev.codes.lpu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nors.dev.codes.lpu.dto.EventRequest;
import org.nors.dev.codes.lpu.dto.EventResponse;
import org.nors.dev.codes.lpu.model.Event;
import org.nors.dev.codes.lpu.repository.EventAttendanceLogRepository;
import org.nors.dev.codes.lpu.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    private static final Logger log = LogManager.getLogger(EventService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Manila");

    private final EventRepository eventRepository;
    private final EventAttendanceLogRepository attendanceLogRepository;
    private final PhotoStorageService photoStorageService;
    private final NotificationService notificationService;

    public EventService(
            EventRepository eventRepository,
            EventAttendanceLogRepository attendanceLogRepository,
            PhotoStorageService photoStorageService,
            NotificationService notificationService
    ) {
        this.eventRepository = eventRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.photoStorageService = photoStorageService;
        this.notificationService = notificationService;
    }

    /**
     * Lists events that start within the given calendar month (Asia/Manila).
     * Defaults to the current month when year/month are omitted.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> list(boolean activeOnly, Integer year, Integer month) {
        YearMonth yearMonth = resolveYearMonth(year, month);
        Instant fromInclusive = yearMonth.atDay(1).atStartOfDay(APP_ZONE).toInstant();
        Instant toExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant();
        return eventRepository.findStartingBetween(fromInclusive, toExclusive, activeOnly).stream()
                .map(EventResponse::from)
                .toList();
    }

    /** Active events whose start time falls on today's calendar date (Asia/Manila). */
    @Transactional(readOnly = true)
    public List<EventResponse> listToday() {
        LocalDate today = LocalDate.now(APP_ZONE);
        Instant dayStart = today.atStartOfDay(APP_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        return eventRepository.findActiveStartingBetween(dayStart, dayEnd).stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        return EventResponse.from(requireEvent(id));
    }

    /** Public lookup for the check-in kiosk (active events only). */
    @Transactional(readOnly = true)
    public EventResponse getActivePublic(Long id) {
        Event event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse create(EventRequest request, Long createdByUserId, MultipartFile photo) {
        validateTimes(request.startsAt(), request.endsAt());

        Event event = new Event();
        applyRequest(event, request);
        if (request.active() != null) {
            event.setActive(request.active());
        } else {
            event.setActive(true);
        }
        if (photo != null && !photo.isEmpty()) {
            event.setPhoto(photoStorageService.store(photo));
        }
        event.setCreatedByUserId(createdByUserId);
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        eventRepository.persist(event);

        log.info("Created event id={} title={}", event.getId(), event.getTitle());
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request, MultipartFile photo) {
        Event event = requireEvent(id);
        validateTimes(request.startsAt(), request.endsAt());
        applyRequest(event, request);
        if (request.active() != null) {
            event.setActive(request.active());
        }
        if (photo != null && !photo.isEmpty()) {
            String previous = event.getPhoto();
            event.setPhoto(photoStorageService.store(photo));
            photoStorageService.deleteIfManaged(previous);
        }
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);

        log.info("Updated event id={} title={}", id, event.getTitle());
        EventResponse response = EventResponse.from(event);
        notificationService.broadcastEventUpdated(id, response);
        return response;
    }

    @Transactional
    public EventResponse setActive(Long id, boolean active) {
        Event event = requireEvent(id);
        event.setActive(active);
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);
        log.info("{} event id={}", active ? "Activated" : "Deactivated", id);
        EventResponse response = EventResponse.from(event);
        notificationService.broadcastEventUpdated(id, response);
        return response;
    }

    /** Permanently removes the event, its attendance logs, and managed photo (superadmin only). */
    @Transactional
    public void delete(Long id) {
        Event event = requireEvent(id);
        int removedLogs = attendanceLogRepository.deleteByEventId(id);
        String photoPath = event.getPhoto();
        eventRepository.delete(event);
        photoStorageService.deleteIfManaged(photoPath);
        log.info("Deleted event id={} title={} attendanceLogs={}", id, event.getTitle(), removedLogs);
    }

    private Event requireEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private static YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.now(APP_ZONE);
        }
        if (year == null || month == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both year and month are required when filtering by month"
            );
        }
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }
        if (year < 1970 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year is out of range");
        }
        try {
            return YearMonth.of(year, month);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year/month");
        }
    }

    private static void applyRequest(Event event, EventRequest request) {
        event.setTitle(normalizeRequired(request.title(), "Title"));
        event.setDescription(blankToNull(request.description()));
        event.setLocation(blankToNull(request.location()));
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
    }

    private static void validateTimes(Instant startsAt, Instant endsAt) {
        if (startsAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
