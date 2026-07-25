package org.nors.dev.codes.lpu.repository;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nors.dev.codes.lpu.model.EventAttendanceLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventAttendanceLogRepository {

    private final SessionFactory sessionFactory;

    public EventAttendanceLogRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional
    public Optional<EventAttendanceLog> findByEventAndStudentForUpdate(Long eventId, Long studentId) {
        return currentSession()
                .createQuery(
                        "FROM EventAttendanceLog l WHERE l.eventId = :eventId AND l.studentId = :studentId",
                        EventAttendanceLog.class
                )
                .setParameter("eventId", eventId)
                .setParameter("studentId", studentId)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .uniqueResultOptional();
    }

    @Transactional
    public Optional<EventAttendanceLog> findByEventAndEmployeeForUpdate(Long eventId, Long employeeId) {
        return currentSession()
                .createQuery(
                        "FROM EventAttendanceLog l WHERE l.eventId = :eventId AND l.employeeId = :employeeId",
                        EventAttendanceLog.class
                )
                .setParameter("eventId", eventId)
                .setParameter("employeeId", employeeId)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceLog> findByEventId(Long eventId, int offset, int limit) {
        return currentSession()
                .createQuery(
                        "FROM EventAttendanceLog l WHERE l.eventId = :eventId ORDER BY l.updatedAt DESC, l.id DESC",
                        EventAttendanceLog.class
                )
                .setParameter("eventId", eventId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public long countByEventId(Long eventId) {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(l.id) FROM EventAttendanceLog l WHERE l.eventId = :eventId",
                        Long.class
                )
                .setParameter("eventId", eventId)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countAll() {
        Long count = currentSession()
                .createQuery("SELECT COUNT(l.id) FROM EventAttendanceLog l", Long.class)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countByPersonType(String personType) {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(l.id) FROM EventAttendanceLog l WHERE l.personType = :personType",
                        Long.class
                )
                .setParameter("personType", personType)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countCurrentlyCheckedIn() {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(l.id) FROM EventAttendanceLog l WHERE l.lastAction = 'TIME_IN'",
                        Long.class
                )
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countByEventIdAndPersonType(Long eventId, String personType) {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(l.id) FROM EventAttendanceLog l "
                                + "WHERE l.eventId = :eventId AND l.personType = :personType",
                        Long.class
                )
                .setParameter("eventId", eventId)
                .setParameter("personType", personType)
                .uniqueResult();
        return count != null ? count : 0;
    }

    /** Returns [eventId, attendeeCount] ordered by attendance desc. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> topEventIdsByAttendance(int limit) {
        return currentSession()
                .createQuery(
                        "SELECT l.eventId, COUNT(l.id) FROM EventAttendanceLog l "
                                + "GROUP BY l.eventId ORDER BY COUNT(l.id) DESC"
                )
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceLog> listByEventId(Long eventId) {
        return currentSession()
                .createQuery(
                        "FROM EventAttendanceLog l WHERE l.eventId = :eventId ORDER BY l.updatedAt DESC, l.id DESC",
                        EventAttendanceLog.class
                )
                .setParameter("eventId", eventId)
                .getResultList();
    }

    @Transactional
    public void persist(EventAttendanceLog log) {
        Session session = currentSession();
        session.persist(log);
        session.flush();
    }

    @Transactional
    public EventAttendanceLog save(EventAttendanceLog log) {
        return currentSession().merge(log);
    }

    @Transactional
    public int deleteByEventId(Long eventId) {
        return currentSession()
                .createMutationQuery("DELETE FROM EventAttendanceLog l WHERE l.eventId = :eventId")
                .setParameter("eventId", eventId)
                .executeUpdate();
    }
}
