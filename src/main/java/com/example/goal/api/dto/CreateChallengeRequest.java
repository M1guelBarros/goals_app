package com.example.goal.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class CreateChallengeRequest {

    @NotBlank
    @Size(max = 30)
    public String title;

    @Size(max = 100)
    public String description;

    public LocalDate startDate;

    @NotNull
    public LocalDate dueDate;

    @NotEmpty
    @Valid
    public List<CreateChallengeGoalRequest> goals;
}
