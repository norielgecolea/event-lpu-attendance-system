package org.nors.dev.codes.lpu.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nors.dev.codes.lpu.dto.AuthEventMessage;
import org.nors.dev.codes.lpu.dto.EventToneResponse;
import org.nors.dev.codes.lpu.dto.EventToneSettingsResponse;
import org.nors.dev.codes.lpu.model.AppSetting;
import org.nors.dev.codes.lpu.model.EventTone;
import org.nors.dev.codes.lpu.repository.AppSettingRepository;
import org.nors.dev.codes.lpu.repository.EventToneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages uploaded event-kiosk tones and which tone is assigned to each event type.
 * Null / missing assignment means the kiosk uses its built-in default sound.
 */
@Service
public class EventToneService {

    public static final List<String> EVENT_TYPES = List.of(
            "TIME_IN",
            "TIME_OUT",
            "ERROR",
            "BIRTHDAY"
    );

    private static final String SETTING_PREFIX = "event.tone.";
    private static final Logger log = LogManager.getLogger(EventToneService.class);

    private final EventToneRepository toneRepository;
    private final AppSettingRepository settingRepository;
    private final ToneStorageService toneStorage;
    private final NotificationService notificationService;

    public EventToneService(
            EventToneRepository toneRepository,
            AppSettingRepository settingRepository,
            ToneStorageService toneStorage,
            NotificationService notificationService
    ) {
        this.toneRepository = toneRepository;
        this.settingRepository = settingRepository;
        this.toneStorage = toneStorage;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public EventToneSettingsResponse getSettings() {
        List<EventToneResponse> tones = toneRepository.findAllOrdered().stream()
                .map(EventToneResponse::from)
                .toList();
        return new EventToneSettingsResponse(tones, readAssignments());
    }

    @Transactional
    public List<EventToneResponse> upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one audio file is required");
        }
        for (MultipartFile file : files) {
            ToneStorageService.StoredTone stored = toneStorage.store(file);
            EventTone tone = new EventTone();
            tone.setFilePath(stored.path());
            tone.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : stored.path());
            tone.setContentType(stored.contentType());
            tone.setSizeBytes(stored.sizeBytes());
            tone.setUploadedAt(Instant.now());
            toneRepository.persist(tone);
        }
        broadcastChange();
        return toneRepository.findAllOrdered().stream().map(EventToneResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        EventTone tone = toneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tone not found"));
        clearAssignmentsForTone(id);
        toneRepository.delete(tone);
        toneStorage.deleteFile(tone.getFilePath());
        broadcastChange();
    }

    @Transactional
    public EventToneSettingsResponse setAssignments(Map<String, Object> body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignments body is required");
        }
        for (String eventType : EVENT_TYPES) {
            if (!body.containsKey(eventType)) {
                continue;
            }
            Object raw = body.get(eventType);
            String rawId = raw == null ? "" : String.valueOf(raw).trim();
            if (rawId.isEmpty() || "null".equalsIgnoreCase(rawId)) {
                clearSetting(SETTING_PREFIX + eventType);
                continue;
            }
            Long toneId;
            try {
                toneId = Long.parseLong(rawId);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tone id for " + eventType);
            }
            if (toneRepository.findById(toneId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tone id for " + eventType);
            }
            saveSetting(SETTING_PREFIX + eventType, String.valueOf(toneId));
        }
        broadcastChange();
        return getSettings();
    }

    private Map<String, String> readAssignments() {
        Map<String, String> assignments = new LinkedHashMap<>();
        for (String eventType : EVENT_TYPES) {
            assignments.put(eventType, settingRepository.findByKey(SETTING_PREFIX + eventType)
                    .map(AppSetting::getValue)
                    .filter(value -> !value.isBlank())
                    .orElse(null));
        }
        return assignments;
    }

    private void clearAssignmentsForTone(Long toneId) {
        String id = String.valueOf(toneId);
        for (String eventType : EVENT_TYPES) {
            String key = SETTING_PREFIX + eventType;
            settingRepository.findByKey(key).ifPresent(setting -> {
                if (id.equals(setting.getValue())) {
                    clearSetting(key);
                }
            });
        }
    }

    private void saveSetting(String key, String value) {
        AppSetting setting = settingRepository.findByKey(key).orElseGet(() -> {
            AppSetting created = new AppSetting();
            created.setKey(key);
            return created;
        });
        setting.setValue(value);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);
    }

    private void clearSetting(String key) {
        settingRepository.findByKey(key).ifPresent(setting -> {
            setting.setValue("");
            setting.setUpdatedAt(Instant.now());
            settingRepository.save(setting);
        });
    }

    private void broadcastChange() {
        notificationService.broadcast(AuthEventMessage.of(
                "EVENT_TONES_CHANGED",
                "Event tones updated"
        ));
        log.info("Broadcast EVENT_TONES_CHANGED");
    }
}
