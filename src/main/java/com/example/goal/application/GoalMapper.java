package com.example.goal.application;

import com.example.goal.api.dto.HistoryResponse;
import com.example.goal.api.dto.GoalResponse;
import com.example.goal.persistence.GoalEntity;
import com.example.goal.persistence.HistoryEntity;

/**
 * Maps persistence entities to API DTOs.
 */
final class GoalMapper {

    private GoalMapper() {
    }

    static GoalResponse toResponse(GoalEntity entity) {
        GoalResponse response = new GoalResponse();
        response.id = entity.id;
        response.title = entity.title;
        response.description = entity.description;
        response.duration = entity.duration;
        response.aimType = entity.aimType;
        response.status = entity.status;
        response.targetCount = entity.targetCount;
        response.currentCount = entity.currentCount;
        response.startDate = entity.startDate;
        response.dueDate = entity.dueDate;
        response.challengeId = entity.challenge == null ? null : entity.challenge.id;
        response.createdAt = entity.createdAt;
        response.updatedAt = entity.updatedAt;
        return response;
    }

    static HistoryResponse toHistoryResponse(HistoryEntity entity) {
        HistoryResponse response = new HistoryResponse();
        response.id = entity.id;
        response.goalId = entity.goal.id;
        response.progressType = entity.progressType;
        response.amount = entity.amount;
        response.resultingCount = entity.resultingCount;
        response.eventDate = entity.eventDate;
        response.createdAt = entity.createdAt;
        return response;
    }
}
