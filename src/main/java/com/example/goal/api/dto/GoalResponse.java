package com.example.goal.api.dto;

import com.example.goal.domain.AimType;
import com.example.goal.domain.Duration;
import com.example.goal.domain.GoalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class GoalResponse {
    public UUID id;
    public String title;
    public String description;
    public Duration duration;
    public AimType aimType;
    public GoalStatus status;
    public int targetCount;
    public int currentCount;
    public LocalDate startDate;
    public LocalDate dueDate;
    public UUID challengeId;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
