package org.nors.dev.codes.lpu.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.nors.dev.codes.lpu.dto.EventAttendanceLogResponse;
import org.nors.dev.codes.lpu.dto.EventAttendancePageResponse;
import org.nors.dev.codes.lpu.dto.EventAttendanceStatsResponse;
import org.nors.dev.codes.lpu.dto.EventAttendanceTapRequest;
import org.nors.dev.codes.lpu.dto.EventKioskStatusResponse;
import org.nors.dev.codes.lpu.security.AuthenticatedUser;
import org.nors.dev.codes.lpu.service.EventAttendanceService;
import org.nors.dev.codes.lpu.service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-attendance")
public class EventAttendanceController {

    private final EventAttendanceService eventAttendanceService;
    private final NotificationService notificationService;

    public EventAttendanceController(
            EventAttendanceService eventAttendanceService,
            NotificationService notificationService
    ) {
        this.eventAttendanceService = eventAttendanceService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<EventAttendancePageResponse> page(
            @RequestParam Long eventId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(eventAttendanceService.pageByEvent(eventId, offset, limit));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EventAttendanceLogResponse>> list(@RequestParam Long eventId) {
        return ResponseEntity.ok(eventAttendanceService.listByEvent(eventId));
    }

    @GetMapping("/stats")
    public ResponseEntity<EventAttendanceStatsResponse> stats(@RequestParam Long eventId) {
        return ResponseEntity.ok(eventAttendanceService.statsByEvent(eventId));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@RequestParam Long eventId) {
        byte[] csv = eventAttendanceService.exportCsv(eventId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"event-" + eventId + "-attendance.csv\""
                )
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/kiosk-status")
    public ResponseEntity<EventKioskStatusResponse> kioskStatus(
            @RequestParam(required = false) Long eventId
    ) {
        return ResponseEntity.ok(notificationService.kioskStatus(eventId));
    }

    @PostMapping("/tap")
    public ResponseEntity<EventAttendanceLogResponse> tap(
            @Valid @RequestBody EventAttendanceTapRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(eventAttendanceService.tap(request.eventId(), request.identifier(), userId));
    }

    /** Public kiosk tap — no login required; still enforces event start/end window. */
    @PostMapping("/public-tap")
    public ResponseEntity<EventAttendanceLogResponse> publicTap(
            @Valid @RequestBody EventAttendanceTapRequest request
    ) {
        return ResponseEntity.ok(eventAttendanceService.tap(request.eventId(), request.identifier(), null));
    }
}
