package org.nors.dev.codes.lpu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Event time-in / time-out log stored in this system's database.
 * {@code studentId} / {@code employeeId} reference gate DB rows (no FK).
 */
@Entity
@Table(name = "event_attendance_logs")
public class EventAttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "person_type", nullable = false, length = 20)
    private String personType;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "person_no", nullable = false, length = 50)
    private String personNo;

    @Column(length = 100)
    private String rfid;

    @Column(name = "time_in", nullable = false)
    private Instant timeIn;

    @Column(name = "time_out")
    private Instant timeOut;

    @Column(name = "last_action", nullable = false, length = 20)
    private String lastAction = "TIME_IN";

    @Column(name = "tapped_by_user_id")
    private Long tappedByUserId;

    @Column(name = "tap_count", nullable = false)
    private int tapCount = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getPersonType() {
        return personType;
    }

    public void setPersonType(String personType) {
        this.personType = personType;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getPersonNo() {
        return personNo;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public Instant getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(Instant timeIn) {
        this.timeIn = timeIn;
    }

    public Instant getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Instant timeOut) {
        this.timeOut = timeOut;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public Long getTappedByUserId() {
        return tappedByUserId;
    }

    public void setTappedByUserId(Long tappedByUserId) {
        this.tappedByUserId = tappedByUserId;
    }

    public int getTapCount() {
        return tapCount;
    }

    public void setTapCount(int tapCount) {
        this.tapCount = tapCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
