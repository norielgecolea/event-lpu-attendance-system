package org.nors.dev.codes.lpu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.nors.dev.codes.lpu.dto.EventRequest;
import org.nors.dev.codes.lpu.dto.EventResponse;
import org.nors.dev.codes.lpu.security.AuthenticatedUser;
import org.nors.dev.codes.lpu.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public EventController(EventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(eventService.list(activeOnly, year, month));
    }

    /** Public: active events starting today (Asia/Manila). */
    @GetMapping("/today")
    public ResponseEntity<List<EventResponse>> today() {
        return ResponseEntity.ok(eventService.listToday());
    }

    /** Public: active event details for the check-in kiosk. */
    @GetMapping("/{id}/public")
    public ResponseEntity<EventResponse> getPublic(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getActivePublic(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> createMultipart(
            @RequestPart("event") String eventJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        EventRequest request = parseEvent(eventJson);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, userId, photo));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponse> createJson(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, userId, null));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OSAS')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> updateMultipart(
            @PathVariable Long id,
            @RequestPart("event") String eventJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return ResponseEntity.ok(eventService.update(id, parseEvent(eventJson), photo));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OSAS')")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponse> updateJson(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {
        return ResponseEntity.ok(eventService.update(id, request, null));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OSAS')")
    @PatchMapping("/{id}/active")
    public ResponseEntity<EventResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean active = body != null ? body.get("active") : null;
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(eventService.setActive(id, active));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Event deleted"));
    }

    private EventRequest parseEvent(String eventJson) {
        try {
            EventRequest request = objectMapper.readValue(eventJson, EventRequest.class);
            if (request.title() == null || request.title().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
            }
            if (request.startsAt() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
            }
            return request;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event payload");
        }
    }
}
