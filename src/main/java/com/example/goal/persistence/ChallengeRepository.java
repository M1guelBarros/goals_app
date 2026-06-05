package com.example.goal.persistence;

import com.example.goal.domain.ChallengeStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ChallengeRepository implements PanacheRepositoryBase<ChallengeEntity, UUID> {

    public Optional<ChallengeEntity> findByIdWithGoals(UUID id) {
        return find("select distinct c from ChallengeEntity c left join fetch c.goals where c.id = ?1", id)
                .firstResultOptional();
    }

    public List<ChallengeEntity> listWithGoals(ChallengeStatus status,
                                                String titleContains,
                                                boolean includeArchived) {
        StringBuilder query = new StringBuilder("select distinct c from ChallengeEntity c left join fetch c.goals where 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            query.append(" and c.status = ?").append(params.size() + 1);
            params.add(status);
        } else if (!includeArchived) {
            query.append(" and c.status <> ?").append(params.size() + 1);
            params.add(ChallengeStatus.ARCHIVED);
        }

        if (titleContains != null && !titleContains.isBlank()) {
            query.append(" and lower(c.title) like ?").append(params.size() + 1);
            params.add("%" + titleContains.toLowerCase() + "%");
        }

        query.append(" order by c.createdAt desc");
        return find(query.toString(), params.toArray()).list();
    }
}
