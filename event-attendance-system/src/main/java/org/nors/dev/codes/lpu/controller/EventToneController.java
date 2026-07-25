package org.nors.dev.codes.lpu.controller;

import java.util.List;
import java.util.Map;
import org.nors.dev.codes.lpu.dto.EventToneResponse;
import org.nors.dev.codes.lpu.dto.EventToneSettingsResponse;
import org.nors.dev.codes.lpu.service.EventToneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/event-tones")
public class EventToneController {

    private final EventToneService eventToneService;

    public EventToneController(EventToneService eventToneService) {
        this.eventToneService = eventToneService;
    }

    @GetMapping
    public ResponseEntity<EventToneSettingsResponse> getSettings() {
        return ResponseEntity.ok(eventToneService.getSettings());
    }

    @PostMapping
    public ResponseEntity<List<EventToneResponse>> upload(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(eventToneService.upload(files));
    }

    @PutMapping("/assignments")
    public ResponseEntity<EventToneSettingsResponse> setAssignments(
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(eventToneService.setAssignments(body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        eventToneService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Tone deleted"));
    }
}
