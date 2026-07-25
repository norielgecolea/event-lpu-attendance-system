package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import org.nors.dev.codes.lpu.model.EventAttendanceLog;

public record EventAttendanceLogResponse(
        String id,
        String eventId,
        String personType,
        String studentId,
        String employeeId,
        String personName,
        String personNo,
        String rfid,
        String personPhoto,
        Instant timeIn,
        Instant timeOut,
        String lastAction,
        String tappedByUserId,
        int tapCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventAttendanceLogResponse from(EventAttendanceLog log) {
        return from(log, null);
    }

    public static EventAttendanceLogResponse from(EventAttendanceLog log, String personPhoto) {
        return new EventAttendanceLogResponse(
                String.valueOf(log.getId()),
                String.valueOf(log.getEventId()),
                log.getPersonType(),
                log.getStudentId() == null ? null : String.valueOf(log.getStudentId()),
                log.getEmployeeId() == null ? null : String.valueOf(log.getEmployeeId()),
                log.getPersonName(),
                log.getPersonNo(),
                log.getRfid(),
                personPhoto,
                log.getTimeIn(),
                log.getTimeOut(),
                log.getLastAction(),
                log.getTappedByUserId() == null ? null : String.valueOf(log.getTappedByUserId()),
                log.getTapCount(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
