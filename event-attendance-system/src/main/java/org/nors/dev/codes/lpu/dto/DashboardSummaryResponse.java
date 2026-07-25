package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        long totalEvents,
        long activeEvents,
        long eventsToday,
        long upcomingEvents,
        long totalStudents,
        long totalEmployees,
        long totalPortalUsers,
        long activePortalUsers,
        long totalCheckIns,
        long studentCheckIns,
        long employeeCheckIns,
        long currentlyCheckedIn,
        List<EventAttendanceStat> topEventsByAttendance,
        List<UpcomingEventStat> upcomingEventList
) {
    public record EventAttendanceStat(
            String eventId,
            String title,
            String location,
            boolean active,
            Instant startsAt,
            long attendees,
            long studentAttendees,
            long employeeAttendees
    ) {}

    public record UpcomingEventStat(
            String eventId,
            String title,
            String location,
            Instant startsAt,
            Instant endsAt,
            boolean active,
            long attendees
    ) {}
}
