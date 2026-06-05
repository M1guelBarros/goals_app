package com.example.goal.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Panache repository for GoalEntity.
 */
@ApplicationScoped
public class GoalRepository implements PanacheRepositoryBase<GoalEntity, UUID> {

    public List<GoalEntity> findByIds(Collection<UUID> ids) {
        return list("id in ?1", ids);
    }
}
