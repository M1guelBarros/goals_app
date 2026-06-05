package com.example.goal.api.dto;

import com.example.goal.domain.AimType;
import com.example.goal.domain.Duration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateGoalRequest {

    @NotBlank
    @Size(max = 30)
    public String title;

    @Size(max = 100)
    public String description;

    @NotNull
    public Duration duration;

    @NotNull
    public AimType aimType;

    public int targetCount;

    public LocalDate startDate;

    public LocalDate dueDate;
}
