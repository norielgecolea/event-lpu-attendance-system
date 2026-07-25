package org.nors.dev.codes.lpu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.nors.dev.codes.lpu.dto.DashboardSummaryResponse;
import org.nors.dev.codes.lpu.dto.DashboardSummaryResponse.EventAttendanceStat;
import org.nors.dev.codes.lpu.dto.DashboardSummaryResponse.UpcomingEventStat;
import org.nors.dev.codes.lpu.model.Event;
import org.nors.dev.codes.lpu.repository.EmployeeRepository;
import org.nors.dev.codes.lpu.repository.EventAttendanceLogRepository;
import org.nors.dev.codes.lpu.repository.EventRepository;
import org.nors.dev.codes.lpu.repository.StudentRepository;
import org.nors.dev.codes.lpu.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Manila");
    private static final String TYPE_STUDENT = "STUDENT";
    private static final String TYPE_EMPLOYEE = "EMPLOYEE";

    private final EventRepository eventRepository;
    private final EventAttendanceLogRepository attendanceLogRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public DashboardService(
            EventRepository eventRepository,
            EventAttendanceLogRepository attendanceLogRepository,
            StudentRepository studentRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now(APP_ZONE);
        Instant dayStart = today.atStartOfDay(APP_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        Instant now = Instant.now();

        long totalStudents = safeGateCount(() -> studentRepository.countActive(""));
        long totalEmployees = safeGateCount(() -> employeeRepository.countActive(""));

        List<EventAttendanceStat> topEvents = buildTopEvents(5);
        List<UpcomingEventStat> upcoming = buildUpcoming(now, 6);

        return new DashboardSummaryResponse(
                eventRepository.countAll(),
                eventRepository.countActive(),
                eventRepository.countActiveStartingBetween(dayStart, dayEnd),
                eventRepository.countUpcomingActive(now),
                totalStudents,
                totalEmployees,
                userRepository.countAll(),
                userRepository.countActive(),
                attendanceLogRepository.countAll(),
                attendanceLogRepository.countByPersonType(TYPE_STUDENT),
                attendanceLogRepository.countByPersonType(TYPE_EMPLOYEE),
                attendanceLogRepository.countCurrentlyCheckedIn(),
                topEvents,
                upcoming
        );
    }

    private List<EventAttendanceStat> buildTopEvents(int limit) {
        List<EventAttendanceStat> stats = new ArrayList<>();
        for (Object[] row : attendanceLogRepository.topEventIdsByAttendance(limit)) {
            Long eventId = (Long) row[0];
            long attendees = ((Number) row[1]).longValue();
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                continue;
            }
            stats.add(new EventAttendanceStat(
                    String.valueOf(event.getId()),
                    event.getTitle(),
                    event.getLocation(),
                    event.isActive(),
                    event.getStartsAt(),
                    attendees,
                    attendanceLogRepository.countByEventIdAndPersonType(eventId, TYPE_STUDENT),
                    attendanceLogRepository.countByEventIdAndPersonType(eventId, TYPE_EMPLOYEE)
            ));
        }
        return stats;
    }

    private List<UpcomingEventStat> buildUpcoming(Instant from, int limit) {
        List<UpcomingEventStat> list = new ArrayList<>();
        for (Event event : eventRepository.findUpcomingActive(from, limit)) {
            list.add(new UpcomingEventStat(
                    String.valueOf(event.getId()),
                    event.getTitle(),
                    event.getLocation(),
                    event.getStartsAt(),
                    event.getEndsAt(),
                    event.isActive(),
                    attendanceLogRepository.countByEventId(event.getId())
            ));
        }
        return list;
    }

    private long safeGateCount(GateCountSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface GateCountSupplier {
        long get();
    }
}
