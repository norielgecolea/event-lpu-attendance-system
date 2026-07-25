-- LPU Event Attendance System — OWN database schema
-- Students / employees / RFID live on the gate attendance Postgres (read-only).
-- This DB owns: event_users, events, event_attendance_logs.
--
-- Apply:
--   docker exec -i event-postgres-db psql -U postgres -d event_attendance < schema.sql

-- ---------------------------------------------------------------------------
-- event_users
-- Default SUPERADMIN (local only — rotate in production):
--   username: superadmin
--   password: SuperAdmin@123
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    location        VARCHAR(100),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO event_users (username, password_hash, role, active)
VALUES (
    'superadmin',
    '$2b$10$wLzyCFwyRlwIcB4ZU0L9q.9tnLT9BOlnds9B8x41tpZlFck9d0ukq',
    'SUPERADMIN',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------------
-- events
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    location            VARCHAR(255),
    photo               VARCHAR(300),
    starts_at           TIMESTAMPTZ  NOT NULL,
    ends_at             TIMESTAMPTZ,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_user_id  BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_active ON events (active);
CREATE INDEX IF NOT EXISTS idx_events_starts_at ON events (starts_at DESC);

-- ---------------------------------------------------------------------------
-- event_attendance_logs
-- student_id / employee_id reference gate DB rows (NO FK — cross-database).
-- Snapshots (person_*) keep logs readable if the gate record changes later.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_attendance_logs (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT       NOT NULL REFERENCES events (id),
    student_id          BIGINT,
    employee_id         BIGINT,
    person_type         VARCHAR(20)  NOT NULL,
    person_name         VARCHAR(255) NOT NULL,
    person_no           VARCHAR(50)  NOT NULL,
    rfid                VARCHAR(100),
    time_in             TIMESTAMPTZ  NOT NULL,
    time_out            TIMESTAMPTZ,
    last_action         VARCHAR(20)  NOT NULL DEFAULT 'TIME_IN',
    tapped_by_user_id   BIGINT,
    tap_count           INT          NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_event_attendance_person CHECK (
        (student_id IS NOT NULL AND employee_id IS NULL AND person_type = 'STUDENT')
        OR (student_id IS NULL AND employee_id IS NOT NULL AND person_type = 'EMPLOYEE')
    ),
    CONSTRAINT chk_event_attendance_action CHECK (last_action IN ('TIME_IN', 'TIME_OUT'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_event_attendance_student
    ON event_attendance_logs (event_id, student_id)
    WHERE student_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_event_attendance_employee
    ON event_attendance_logs (event_id, employee_id)
    WHERE employee_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_event_attendance_event
    ON event_attendance_logs (event_id, updated_at DESC);
