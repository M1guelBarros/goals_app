package com.example.goal.api.dto;

import com.example.goal.domain.Duration;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateGoalRequest {

    @Size(max = 30)
    public String title;

    @Size(max = 100)
    public String description;

    public Duration duration;

    public LocalDate startDate;

    public LocalDate dueDate;
}
