package org.nors.dev.codes.lpu.dto;

import java.util.List;

public record EventAttendancePageResponse(
        List<EventAttendanceLogResponse> items,
        long total
) {
}
