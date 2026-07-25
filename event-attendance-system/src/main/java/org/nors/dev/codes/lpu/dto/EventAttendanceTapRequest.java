package org.nors.dev.codes.lpu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventAttendanceTapRequest(
        @NotNull(message = "Event ID is required")
        Long eventId,

        /** RFID tag or student number. */
        @NotBlank(message = "RFID or student ID is required")
        String identifier
) {
}
