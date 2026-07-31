package org.nors.dev.codes.lpu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** An unrecognized RFID / ID tap at an event kiosk (record not found). */
@Entity
@Table(name = "tap_error_logs")
public class TapErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String identifier;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_title", length = 255)
    private String eventTitle;

    @Column(length = 255)
    private String location;

    @Column(name = "tapped_at", nullable = false)
    private Instant tappedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getTappedAt() {
        return tappedAt;
    }

    public void setTappedAt(Instant tappedAt) {
        this.tappedAt = tappedAt;
    }
}
