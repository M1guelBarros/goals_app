package com.example.goal.persistence;

import com.example.goal.domain.AimType;
import com.example.goal.domain.Duration;
import com.example.goal.domain.GoalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Main persistence model for a goal.
 *
 * JPA maps this class to a single goals table. We keep counters in this row for fast reads,
 * while detailed action history is stored separately in HistoryEntity.
 */
@Entity
@Table(name = "goals")
public class GoalEntity {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false, length = 30)
    public String title;

    @Column(length = 100)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Duration duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    public AimType aimType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public GoalStatus status;

    @Column(nullable = false)
    public int targetCount;

    @Column(nullable = false)
    public int currentCount;

    @Column
    public LocalDate startDate;

    @Column
    public LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    public ChallengeEntity challenge;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    /**
     * Optimistic locking protects concurrent progress/undo updates.
     */
    @Version
    public long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
