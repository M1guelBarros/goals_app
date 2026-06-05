package com.example.goal.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateChallengeRequest {

    @Size(max = 30)
    public String title;

    @Size(max = 100)
    public String description;

    public LocalDate dueDate;
}
