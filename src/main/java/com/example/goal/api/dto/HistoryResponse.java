package com.example.goal.api.dto;

import com.example.goal.domain.ProgressType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class HistoryResponse {
    public UUID id;
    public UUID goalId;
    public ProgressType progressType;
    public int amount;
    public int resultingCount;
    public LocalDate eventDate;
    public LocalDateTime createdAt;
}
