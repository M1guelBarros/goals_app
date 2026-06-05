package com.example.goal.api.dto;

import com.example.goal.domain.ChallengeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChallengeResponse {
    public UUID id;
    public String title;
    public String description;
    public LocalDate startDate;
    public LocalDate dueDate;
    public ChallengeStatus status;
    public List<GoalResponse> goals;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
