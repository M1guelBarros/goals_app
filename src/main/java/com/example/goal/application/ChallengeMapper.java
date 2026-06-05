package com.example.goal.application;

import com.example.goal.api.dto.ChallengeResponse;
import com.example.goal.persistence.ChallengeEntity;

final class ChallengeMapper {

    private ChallengeMapper() {
    }

    static ChallengeResponse toResponse(ChallengeEntity entity) {
        ChallengeResponse response = new ChallengeResponse();
        response.id = entity.id;
        response.title = entity.title;
        response.description = entity.description;
        response.startDate = entity.startDate;
        response.dueDate = entity.dueDate;
        response.status = entity.status;
        response.goals = entity.goals.stream()
                .map(GoalMapper::toResponse)
                .toList();
        response.createdAt = entity.createdAt;
        response.updatedAt = entity.updatedAt;
        return response;
    }
}
