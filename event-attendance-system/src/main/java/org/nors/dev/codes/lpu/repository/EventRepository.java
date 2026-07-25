package org.nors.dev.codes.lpu.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nors.dev.codes.lpu.model.Event;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventRepository {

    private final SessionFactory sessionFactory;

    public EventRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        return currentSession()
                .createQuery("FROM Event e ORDER BY e.startsAt DESC, e.id DESC", Event.class)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Event> findAllActive() {
        return currentSession()
                .createQuery(
                        "FROM Event e WHERE e.active = true ORDER BY e.startsAt DESC, e.id DESC",
                        Event.class
                )
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Event> findActiveStartingBetween(Instant fromInclusive, Instant toExclusive) {
        return currentSession()
                .createQuery(
                        "FROM Event e WHERE e.active = true "
                                + "AND e.startsAt >= :fromInclusive AND e.startsAt < :toExclusive "
                                + "ORDER BY e.startsAt ASC, e.id ASC",
                        Event.class
                )
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return Optional.ofNullable(currentSession().find(Event.class, id));
    }

    @Transactional(readOnly = true)
    public Optional<Event> findActiveById(Long id) {
        return currentSession()
                .createQuery("FROM Event e WHERE e.id = :id AND e.active = true", Event.class)
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        Long count = currentSession()
                .createQuery("SELECT COUNT(e.id) FROM Event e", Long.class)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countActive() {
        Long count = currentSession()
                .createQuery("SELECT COUNT(e.id) FROM Event e WHERE e.active = true", Long.class)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countActiveStartingBetween(Instant fromInclusive, Instant toExclusive) {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(e.id) FROM Event e WHERE e.active = true "
                                + "AND e.startsAt >= :fromInclusive AND e.startsAt < :toExclusive",
                        Long.class
                )
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public long countUpcomingActive(Instant fromInclusive) {
        Long count = currentSession()
                .createQuery(
                        "SELECT COUNT(e.id) FROM Event e WHERE e.active = true AND e.startsAt >= :fromInclusive",
                        Long.class
                )
                .setParameter("fromInclusive", fromInclusive)
                .uniqueResult();
        return count != null ? count : 0;
    }

    @Transactional(readOnly = true)
    public List<Event> findUpcomingActive(Instant fromInclusive, int limit) {
        return currentSession()
                .createQuery(
                        "FROM Event e WHERE e.active = true AND e.startsAt >= :fromInclusive "
                                + "ORDER BY e.startsAt ASC, e.id ASC",
                        Event.class
                )
                .setParameter("fromInclusive", fromInclusive)
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional
    public void persist(Event event) {
        Session session = currentSession();
        session.persist(event);
        session.flush();
    }

    @Transactional
    public Event save(Event event) {
        return currentSession().merge(event);
    }
}
