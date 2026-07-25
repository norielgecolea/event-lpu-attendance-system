package org.nors.dev.codes.lpu.model;

public enum Role {
    SUPERADMIN,
    /** Office of Student Affairs & Services — events + student records; can deactivate events. */
    OSAS,
    /** Can create events and view attendance (no ID/RFID); cannot edit or deactivate. */
    EVENT_MAKER
}
