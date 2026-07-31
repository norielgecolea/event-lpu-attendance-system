package org.nors.dev.codes.lpu.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nors.dev.codes.lpu.dto.EventAttendanceLogResponse;
import org.nors.dev.codes.lpu.dto.EventAttendancePageResponse;
import org.nors.dev.codes.lpu.dto.EventAttendanceStatsResponse;
import org.nors.dev.codes.lpu.dto.EventAttendanceTapRequest;
import org.nors.dev.codes.lpu.dto.EventKioskStatusResponse;
import org.nors.dev.codes.lpu.dto.TapErrorLogResponse;
import org.nors.dev.codes.lpu.model.Employee;
import org.nors.dev.codes.lpu.model.Event;
import org.nors.dev.codes.lpu.model.EventAttendanceLog;
import org.nors.dev.codes.lpu.model.Student;
import org.nors.dev.codes.lpu.repository.EmployeeRepository;
import org.nors.dev.codes.lpu.repository.EventAttendanceLogRepository;
import org.nors.dev.codes.lpu.repository.EventRepository;
import org.nors.dev.codes.lpu.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventAttendanceService {

    private static final Logger log = LogManager.getLogger(EventAttendanceService.class);
    private static final String ACTION_IN = "TIME_IN";
    private static final String ACTION_OUT = "TIME_OUT";
    private static final String TYPE_STUDENT = "STUDENT";
    private static final String TYPE_EMPLOYEE = "EMPLOYEE";
    /** Ignore rapid re-taps and return the previous transaction instead. */
    private static final Duration TAP_COOLDOWN = Duration.ofSeconds(10);

    private final EventRepository eventRepository;
    private final EventAttendanceLogRepository attendanceLogRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final TapErrorLogService tapErrorLogService;

    public EventAttendanceService(
            EventRepository eventRepository,
            EventAttendanceLogRepository attendanceLogRepository,
            StudentRepository studentRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService,
            TapErrorLogService tapErrorLogService
    ) {
        this.eventRepository = eventRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.tapErrorLogService = tapErrorLogService;
    }

    @Transactional(readOnly = true)
    public EventAttendancePageResponse pageByEvent(Long eventId, int offset, int limit, boolean hideIdentifiers) {
        requireEvent(eventId);
        int size = Math.min(Math.max(limit, 1), 200);
        int from = Math.max(offset, 0);
        List<EventAttendanceLogResponse> items = attendanceLogRepository.findByEventId(eventId, from, size).stream()
                .map(logEntry -> {
                    EventAttendanceLogResponse response = EventAttendanceLogResponse.from(logEntry);
                    return hideIdentifiers ? response.withoutIdentifiers() : response;
                })
                .toList();
        return new EventAttendancePageResponse(items, attendanceLogRepository.countByEventId(eventId));
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceLogResponse> listByEvent(Long eventId, boolean hideIdentifiers) {
        requireEvent(eventId);
        return attendanceLogRepository.listByEventId(eventId).stream()
                .map(logEntry -> {
                    EventAttendanceLogResponse response = EventAttendanceLogResponse.from(logEntry);
                    return hideIdentifiers ? response.withoutIdentifiers() : response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EventAttendanceStatsResponse statsByEvent(Long eventId) {
        requireEvent(eventId);
        List<EventAttendanceLog> logs = attendanceLogRepository.listByEventId(eventId);

        long students = 0;
        long employees = 0;
        long checkedIn = 0;
        long completed = 0;
        long staySumSeconds = 0;
        long stayCount = 0;
        Instant firstCheckIn = null;
        Instant lastActivity = null;
        long[] hourly = new long[24];
        ZoneId zone = ZoneId.of("Asia/Manila");

        for (EventAttendanceLog logEntry : logs) {
            if (TYPE_EMPLOYEE.equals(logEntry.getPersonType())) {
                employees++;
            } else {
                students++;
            }
            if (ACTION_IN.equals(logEntry.getLastAction())) {
                checkedIn++;
            }
            if (logEntry.getTimeOut() != null) {
                completed++;
            }
            if (logEntry.getTimeIn() != null && logEntry.getTimeOut() != null) {
                long seconds = Duration.between(logEntry.getTimeIn(), logEntry.getTimeOut()).getSeconds();
                if (seconds >= 0) {
                    staySumSeconds += seconds;
                    stayCount++;
                }
            }
            if (logEntry.getTimeIn() != null) {
                if (firstCheckIn == null || logEntry.getTimeIn().isBefore(firstCheckIn)) {
                    firstCheckIn = logEntry.getTimeIn();
                }
                int hour = logEntry.getTimeIn().atZone(zone).getHour();
                hourly[hour]++;
            }
            Instant updated = logEntry.getUpdatedAt() != null ? logEntry.getUpdatedAt() : logEntry.getTimeIn();
            if (updated != null && (lastActivity == null || updated.isAfter(lastActivity))) {
                lastActivity = updated;
            }
        }

        Double averageStayMinutes = stayCount == 0
                ? null
                : Math.round((staySumSeconds / (double) stayCount / 60.0) * 10.0) / 10.0;

        List<EventAttendanceStatsResponse.HourlyBucket> buckets = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            if (hourly[hour] == 0) {
                continue;
            }
            String label = String.format("%02d:00", hour);
            buckets.add(new EventAttendanceStatsResponse.HourlyBucket(hour, hourly[hour], label));
        }

        return new EventAttendanceStatsResponse(
                logs.size(),
                students,
                employees,
                checkedIn,
                completed,
                averageStayMinutes,
                firstCheckIn,
                lastActivity,
                buckets
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(Long eventId, boolean hideIdentifiers) {
        Event event = requireEvent(eventId);
        List<EventAttendanceLog> logs = attendanceLogRepository.listByEventId(eventId);
        ZoneId zone = ZoneId.of("Asia/Manila");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            if (hideIdentifiers) {
                writer.println("Event,Name,Type,Time In,Time Out,Last Action,Tap Count");
            } else {
                writer.println("Event,Name,ID Number,Type,RFID,Time In,Time Out,Last Action,Tap Count");
            }
            for (EventAttendanceLog logEntry : logs) {
                if (hideIdentifiers) {
                    writer.printf(
                            "%s,%s,%s,%s,%s,%s,%d%n",
                            csv(event.getTitle()),
                            csv(logEntry.getPersonName()),
                            csv(logEntry.getPersonType()),
                            logEntry.getTimeIn() == null ? "" : formatter.format(logEntry.getTimeIn()),
                            logEntry.getTimeOut() == null ? "" : formatter.format(logEntry.getTimeOut()),
                            csv(logEntry.getLastAction()),
                            logEntry.getTapCount()
                    );
                } else {
                    writer.printf(
                            "%s,%s,%s,%s,%s,%s,%s,%s,%d%n",
                            csv(event.getTitle()),
                            csv(logEntry.getPersonName()),
                            csv(logEntry.getPersonNo()),
                            csv(logEntry.getPersonType()),
                            csv(logEntry.getRfid()),
                            logEntry.getTimeIn() == null ? "" : formatter.format(logEntry.getTimeIn()),
                            logEntry.getTimeOut() == null ? "" : formatter.format(logEntry.getTimeOut()),
                            csv(logEntry.getLastAction()),
                            logEntry.getTapCount()
                    );
                }
            }
        }
        return baos.toByteArray();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceLogResponse> recent(int limit, boolean hideIdentifiers) {
        int size = Math.min(Math.max(limit, 1), 50);
        List<EventAttendanceLog> logs = attendanceLogRepository.findRecent(size);
        return logs.stream()
                .map(logEntry -> {
                    Event event = eventRepository.findById(logEntry.getEventId()).orElse(null);
                    String photo = resolvePersonPhoto(logEntry);
                    EventAttendanceLogResponse response = EventAttendanceLogResponse.from(logEntry, photo)
                            .withEvent(
                                    event != null ? event.getTitle() : null,
                                    event != null ? event.getLocation() : null
                            );
                    return hideIdentifiers ? response.withoutIdentifiers() : response;
                })
                .toList();
    }

    private String resolvePersonPhoto(EventAttendanceLog logEntry) {
        if (TYPE_STUDENT.equals(logEntry.getPersonType()) && logEntry.getStudentId() != null) {
            return studentRepository.findById(logEntry.getStudentId())
                    .map(Student::getPhoto)
                    .orElse(null);
        }
        if (TYPE_EMPLOYEE.equals(logEntry.getPersonType()) && logEntry.getEmployeeId() != null) {
            return employeeRepository.findById(logEntry.getEmployeeId())
                    .map(Employee::getPhoto)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Toggle TIME_IN / TIME_OUT for a student or employee at an event.
     * Identifier may be RFID, student number, or employee number (looked up on the gate DB).
     */
    @Transactional
    public EventAttendanceLogResponse tap(Long eventId, String rawIdentifier, Long tappedByUserId) {
        Event event = requireActiveEvent(eventId);
        String identifier = rawIdentifier == null ? "" : rawIdentifier.trim();
        if (identifier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RFID or ID number is required");
        }

        Instant now = Instant.now();
        if (now.isBefore(event.getStartsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance is not open yet. This event has not started."
            );
        }
        if (event.getEndsAt() != null && now.isAfter(event.getEndsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance is closed. This event has already ended."
            );
        }

        Student student = studentRepository.findByRfidOrStudentNo(identifier).orElse(null);
        Employee employee = student == null
                ? employeeRepository.findByRfidOrEmployeeNo(identifier).orElse(null)
                : null;
        if (student == null && employee == null) {
            broadcastTapError(identifier, event);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No student or employee matched that RFID/ID");
        }

        EventAttendanceLog existing;
        EventAttendanceLog result;

        if (student != null) {
            existing = attendanceLogRepository
                    .findByEventAndStudentForUpdate(event.getId(), student.getId())
                    .orElse(null);
            if (isWithinCooldown(existing, now)) {
                log.info("TAP cooldown eventId={} studentNo={}", eventId, student.getStudentNo());
                return enrichWithEvent(
                        EventAttendanceLogResponse.from(
                                existing,
                                student.getPhoto(),
                                student.getBirthdate(),
                                true
                        ),
                        event
                );
            }
            result = applyTap(
                    existing,
                    event.getId(),
                    TYPE_STUDENT,
                    student.getId(),
                    null,
                    student.getName(),
                    student.getStudentNo(),
                    student.getRfid(),
                    now,
                    tappedByUserId
            );
            log.info("{} eventId={} studentNo={}", result.getLastAction(), eventId, student.getStudentNo());
            EventAttendanceLogResponse response = enrichWithEvent(
                    EventAttendanceLogResponse.from(
                            result,
                            student.getPhoto(),
                            student.getBirthdate()
                    ),
                    event
            );
            notificationService.broadcastAttendanceTap(response);
            return response;
        }

        existing = attendanceLogRepository
                .findByEventAndEmployeeForUpdate(event.getId(), employee.getId())
                .orElse(null);
        if (isWithinCooldown(existing, now)) {
            log.info("TAP cooldown eventId={} employeeNo={}", eventId, employee.getEmployeeNo());
            return enrichWithEvent(
                    EventAttendanceLogResponse.from(
                            existing,
                            employee.getPhoto(),
                            employee.getBirthdate(),
                            true
                    ),
                    event
            );
        }
        result = applyTap(
                existing,
                event.getId(),
                TYPE_EMPLOYEE,
                null,
                employee.getId(),
                employee.getName(),
                employee.getEmployeeNo(),
                employee.getRfid(),
                now,
                tappedByUserId
        );
        log.info("{} eventId={} employeeNo={}", result.getLastAction(), eventId, employee.getEmployeeNo());
        EventAttendanceLogResponse response = enrichWithEvent(
                EventAttendanceLogResponse.from(
                        result,
                        employee.getPhoto(),
                        employee.getBirthdate()
                ),
                event
        );
        notificationService.broadcastAttendanceTap(response);
        return response;
    }

    private void broadcastTapError(String identifier, Event event) {
        TapErrorLogResponse logged = tapErrorLogService.record(
                identifier,
                event.getId(),
                event.getTitle(),
                event.getLocation()
        );
        notificationService.broadcastAttendanceTapError(logged);
    }

    private static EventAttendanceLogResponse enrichWithEvent(
            EventAttendanceLogResponse response,
            Event event
    ) {
        return response.withEvent(event.getTitle(), event.getLocation());
    }

    private static boolean isWithinCooldown(EventAttendanceLog existing, Instant now) {
        return existing != null
                && existing.getUpdatedAt() != null
                && now.isBefore(existing.getUpdatedAt().plus(TAP_COOLDOWN));
    }

    private EventAttendanceLog applyTap(
            EventAttendanceLog existing,
            Long eventId,
            String personType,
            Long studentId,
            Long employeeId,
            String personName,
            String personNo,
            String rfid,
            Instant now,
            Long tappedByUserId
    ) {
        if (existing == null) {
            EventAttendanceLog created = new EventAttendanceLog();
            created.setEventId(eventId);
            created.setStudentId(studentId);
            created.setEmployeeId(employeeId);
            created.setPersonType(personType);
            created.setPersonName(personName);
            created.setPersonNo(personNo);
            created.setRfid(rfid);
            created.setTimeIn(now);
            created.setTimeOut(null);
            created.setLastAction(ACTION_IN);
            created.setTappedByUserId(tappedByUserId);
            created.setTapCount(1);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            attendanceLogRepository.persist(created);
            return created;
        }

        if (ACTION_OUT.equals(existing.getLastAction())) {
            existing.setLastAction(ACTION_IN);
        } else {
            existing.setTimeOut(now);
            existing.setLastAction(ACTION_OUT);
        }
        existing.setPersonName(personName);
        existing.setPersonNo(personNo);
        existing.setRfid(rfid);
        existing.setTappedByUserId(tappedByUserId);
        existing.setTapCount(existing.getTapCount() + 1);
        existing.setUpdatedAt(now);
        return attendanceLogRepository.save(existing);
    }

    private Event requireEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private Event requireActiveEvent(Long eventId) {
        return eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active event not found"));
    }
}
