package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        boolean birthday,
        boolean duplicate,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Manila");

    public static EventAttendanceLogResponse from(EventAttendanceLog log) {
        return from(log, null, null, false);
    }

    public static EventAttendanceLogResponse from(EventAttendanceLog log, String personPhoto) {
        return from(log, personPhoto, null, false);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate
    ) {
        return from(log, personPhoto, birthdate, false);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate,
            boolean duplicate
    ) {
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
                isBirthdayToday(birthdate),
                duplicate,
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    private static boolean isBirthdayToday(LocalDate birthdate) {
        if (birthdate == null) {
            return false;
        }
        LocalDate today = LocalDate.now(APP_ZONE);
        return birthdate.getMonthValue() == today.getMonthValue()
                && birthdate.getDayOfMonth() == today.getDayOfMonth();
    }
}
