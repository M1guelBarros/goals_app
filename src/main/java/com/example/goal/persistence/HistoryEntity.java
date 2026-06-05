package com.example.goal.persistence;

import com.example.goal.domain.ProgressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only history of user actions that change goal progress.
 *
 * eventDate is business time (what day the user says they performed work),
 * while createdAt is system time (when the record was persisted).
 */
@Entity
@Table(name = "goal_progress_records")
public class HistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    public GoalEntity goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24, updatable = false)
    public ProgressType progressType;

    @Column(nullable = false, updatable = false)
    public int amount;

    @Column(nullable = false, updatable = false)
    public int resultingCount;

    @Column(nullable = false, updatable = false)
    public LocalDate eventDate;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
