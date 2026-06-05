package com.example.goal.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for append-only goal progress history.
 */
@ApplicationScoped
public class HistoryRepository implements PanacheRepositoryBase<HistoryEntity, UUID> {

    public List<HistoryEntity> findHistoryByGoalId(UUID goalId) {
        return find("goal.id = ?1 order by createdAt asc", goalId).list();
    }

    public Optional<HistoryEntity> findLatestByGoalId(UUID goalId) {
        return find("goal.id = ?1 order by createdAt desc", goalId).firstResultOptional();
    }

    public long countByGoalId(UUID goalId) {
        return count("goal.id", goalId);
    }
}
