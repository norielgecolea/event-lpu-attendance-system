package org.nors.dev.codes.lpu.repository;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nors.dev.codes.lpu.model.EventTone;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventToneRepository {

    private final SessionFactory sessionFactory;

    public EventToneRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional(readOnly = true)
    public List<EventTone> findAllOrdered() {
        return currentSession()
                .createQuery("FROM EventTone t ORDER BY t.uploadedAt DESC, t.id DESC", EventTone.class)
                .list();
    }

    @Transactional(readOnly = true)
    public Optional<EventTone> findById(Long id) {
        return Optional.ofNullable(currentSession().find(EventTone.class, id));
    }

    @Transactional
    public void persist(EventTone tone) {
        currentSession().persist(tone);
    }

    @Transactional
    public void delete(EventTone tone) {
        currentSession().remove(tone);
    }
}
