# LPU Event Attendance System

Separate event check-in system. It keeps its **own database** for users, events, and time logs, and connects **read-only** to the gate attendance Postgres for students, employees, and RFID.

## Databases

| Database | Owns |
|----------|------|
| **This system** (`POSTGRES_*`) | `event_users`, `events`, `event_attendance_logs` |
| **Gate attendance** (`GATE_ATTENDANCE_DB_HOST`) | `students`, `employees` (+ RFID) — read-only |

## Env (`.env`)

```bash
# Required: IP/hostname of the gate attendance Postgres
GATE_ATTENDANCE_DB_HOST=192.168.1.50
# Required for photo proxy — must include http:// or https://
GATE_ATTENDANCE_URL=http://192.168.1.50

# This system's DB
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_DB=postgres
POSTGRES_PORT=5433
```

Optional gate overrides: `GATE_ATTENDANCE_DB_PORT`, `GATE_ATTENDANCE_DB_NAME`, `GATE_ATTENDANCE_DB_USER`, `GATE_ATTENDANCE_DB_PASSWORD`, `GATE_ATTENDANCE_URL` (photos).

## Run

```bash
cp .env.example .env
# set GATE_ATTENDANCE_DB_HOST to the gate DB IP

mvn -f event-attendance-system clean package
cp event-attendance-system/target/event-attendance-system.war data/tomcat/webapps/

docker compose up -d --build
# → http://localhost  /  https://localhost
# TLS: data/web/certs is gitignored; nginx auto-creates a self-signed cert on first start.
```

Login: `superadmin` / `SuperAdmin@123`

## Superadmin pages

- `/dashboard` — summary
- `/events` — create / edit events
- `/events/:id/attendance` — time-in / time-out logs (stored locally)
- `/students` — gate student records (read-only)
- `/employees` — gate employee records (read-only)
- `/users` — event-system accounts
