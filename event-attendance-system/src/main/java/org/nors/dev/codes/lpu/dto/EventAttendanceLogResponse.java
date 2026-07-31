package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.nors.dev.codes.lpu.model.EventAttendanceLog;

public record EventAttendanceLogResponse(
        String id,
        String eventId,
        String eventTitle,
        String eventLocation,
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
        return from(log, null, null, false, false, null, null);
    }

    public static EventAttendanceLogResponse from(EventAttendanceLog log, String personPhoto) {
        return from(log, personPhoto, null, false, false, null, null);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate
    ) {
        return from(log, personPhoto, birthdate, false, false, null, null);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate,
            boolean duplicate
    ) {
        return from(log, personPhoto, birthdate, duplicate, false, null, null);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate,
            boolean duplicate,
            boolean hideIdentifiers
    ) {
        return from(log, personPhoto, birthdate, duplicate, hideIdentifiers, null, null);
    }

    public static EventAttendanceLogResponse from(
            EventAttendanceLog log,
            String personPhoto,
            LocalDate birthdate,
            boolean duplicate,
            boolean hideIdentifiers,
            String eventTitle,
            String eventLocation
    ) {
        return new EventAttendanceLogResponse(
                String.valueOf(log.getId()),
                String.valueOf(log.getEventId()),
                eventTitle,
                eventLocation,
                log.getPersonType(),
                log.getStudentId() == null ? null : String.valueOf(log.getStudentId()),
                log.getEmployeeId() == null ? null : String.valueOf(log.getEmployeeId()),
                log.getPersonName(),
                hideIdentifiers ? null : log.getPersonNo(),
                hideIdentifiers ? null : log.getRfid(),
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

    public EventAttendanceLogResponse withEvent(String title, String location) {
        return new EventAttendanceLogResponse(
                id,
                eventId,
                title,
                location,
                personType,
                studentId,
                employeeId,
                personName,
                personNo,
                rfid,
                personPhoto,
                timeIn,
                timeOut,
                lastAction,
                tappedByUserId,
                tapCount,
                birthday,
                duplicate,
                createdAt,
                updatedAt
        );
    }

    /** Strips ID number and RFID for roles that must not see them (e.g. EVENT_MAKER). */
    public EventAttendanceLogResponse withoutIdentifiers() {
        return new EventAttendanceLogResponse(
                id,
                eventId,
                eventTitle,
                eventLocation,
                personType,
                studentId,
                employeeId,
                personName,
                null,
                null,
                personPhoto,
                timeIn,
                timeOut,
                lastAction,
                tappedByUserId,
                tapCount,
                birthday,
                duplicate,
                createdAt,
                updatedAt
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
