package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.util.List;

public record EventAttendanceStatsResponse(
        long totalAttendees,
        long studentAttendees,
        long employeeAttendees,
        long currentlyCheckedIn,
        long completedVisits,
        Double averageStayMinutes,
        Instant firstCheckIn,
        Instant lastActivity,
        List<HourlyBucket> checkInsByHour
) {
    public record HourlyBucket(int hour, long count, String label) {}
}
