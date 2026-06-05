package com.example.goal.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class AddChallengeGoalsRequest {

    @NotEmpty
    public List<@NotNull UUID> goalIds;

    public boolean confirmDateChange;
}
