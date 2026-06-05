package com.example.goal.application;

import com.example.goal.api.dto.AddChallengeGoalsRequest;
import com.example.goal.api.dto.ChallengeResponse;
import com.example.goal.api.dto.CreateChallengeGoalRequest;
import com.example.goal.api.dto.CreateChallengeRequest;
import com.example.goal.api.dto.UpdateChallengeRequest;
import com.example.goal.domain.AimType;
import com.example.goal.domain.ChallengeStatus;
import com.example.goal.domain.GoalStatus;
import com.example.goal.persistence.ChallengeEntity;
import com.example.goal.persistence.ChallengeRepository;
import com.example.goal.persistence.GoalEntity;
import com.example.goal.persistence.GoalRepository;
import com.example.goal.shared.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final GoalRepository goalRepository;

    public ChallengeService(ChallengeRepository challengeRepository, GoalRepository goalRepository) {
        this.challengeRepository = challengeRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional
    public ChallengeResponse create(CreateChallengeRequest request) {
        LocalDate challengeStartDate = request.startDate == null ? LocalDate.now() : request.startDate;
        validateCreateRequest(request, challengeStartDate);

        ChallengeEntity challenge = new ChallengeEntity();
        challenge.title = normalizeTitle(request.title);
        challenge.description = normalizeDescription(request.description);
        challenge.startDate = challengeStartDate;
        challenge.dueDate = request.dueDate;
        challenge.status = ChallengeStatus.ACTIVE;

        for (CreateChallengeGoalRequest goalRequest : request.goals) {
            GoalEntity goal = new GoalEntity();
            populateGoalFromChallengeRequest(goal, goalRequest, challenge.startDate, challenge.dueDate);
            goal.challenge = challenge;
            challenge.goals.add(goal);
        }

        challengeRepository.persist(challenge);
        return ChallengeMapper.toResponse(challenge);
    }

    public ChallengeResponse get(UUID id) {
        return ChallengeMapper.toResponse(findOrThrowWithGoals(id));
    }

    public List<ChallengeResponse> list(ChallengeStatus status, String titleContains, boolean includeArchived) {
        return challengeRepository.listWithGoals(status, titleContains, includeArchived)
                .stream()
                .map(ChallengeMapper::toResponse)
                .toList();
    }

    @Transactional
    public ChallengeResponse update(UUID id, UpdateChallengeRequest request) {
        ChallengeEntity challenge = findOrThrowWithGoals(id);
        ensureNotArchived(challenge);

        if (request.title != null) {
            challenge.title = normalizeTitle(request.title);
        }
        if (request.description != null) {
            challenge.description = normalizeDescription(request.description);
        }
        if (request.dueDate != null) {
            validateDueDateNotPast(request.dueDate);
            validateDateRange(challenge.startDate, request.dueDate);
            challenge.dueDate = request.dueDate;
            for (GoalEntity goal : challenge.goals) {
                goal.dueDate = request.dueDate;
            }
        }

        recomputeStoredStatus(challenge);
        return ChallengeMapper.toResponse(challenge);
    }

    @Transactional
    public ChallengeResponse addExistingGoals(UUID id, AddChallengeGoalsRequest request) {
        ChallengeEntity challenge = findOrThrowWithGoals(id);
        ensureNotArchived(challenge);

        if (request == null || request.goalIds == null || request.goalIds.isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "goalIds must contain at least one goal id");
        }

        Set<UUID> uniqueIds = new HashSet<>(request.goalIds);
        List<GoalEntity> goals = goalRepository.findByIds(uniqueIds);
        if (goals.size() != uniqueIds.size()) {
            throw new ApiException(Response.Status.NOT_FOUND, "One or more goals were not found");
        }

        for (GoalEntity goal : goals) {
            if (goal.status == GoalStatus.ARCHIVED) {
                throw new ApiException(Response.Status.CONFLICT,
                        "Archived goals cannot be added to a challenge: " + goal.id);
            }
            if (goal.challenge != null) {
                throw new ApiException(Response.Status.CONFLICT,
                        "Goal already belongs to a challenge: " + goal.id);
            }
            boolean startDateDiffers = !Objects.equals(goal.startDate, challenge.startDate);
            boolean dueDateDiffers = !Objects.equals(goal.dueDate, challenge.dueDate);
            if ((startDateDiffers || dueDateDiffers) && !request.confirmDateChange) {
                throw new ApiException(Response.Status.CONFLICT,
                        "Goal " + goal.id + " dates must match challenge dates. "
                                + "Set confirmDateChange=true to align goal startDate " + goal.startDate
                                + " and dueDate " + goal.dueDate + " with challenge startDate "
                                + challenge.startDate + " and dueDate " + challenge.dueDate);
            }
        }

        for (GoalEntity goal : goals) {
            goal.challenge = challenge;
            goal.startDate = challenge.startDate;
            goal.dueDate = challenge.dueDate;
            validateDateRange(goal.startDate, goal.dueDate);
            challenge.goals.add(goal);
        }

        recomputeStoredStatus(challenge);
        return ChallengeMapper.toResponse(challenge);
    }

    @Transactional
    public ChallengeResponse archive(UUID id) {
        ChallengeEntity challenge = findOrThrowWithGoals(id);
        challenge.status = ChallengeStatus.ARCHIVED;
        for (GoalEntity goal : challenge.goals) {
            goal.status = GoalStatus.ARCHIVED;
        }
        return ChallengeMapper.toResponse(challenge);
    }

    @Transactional
    public void recomputeStatusForGoal(UUID goalId) {
        GoalEntity goal = goalRepository.findByIdOptional(goalId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "Goal not found: " + goalId));
        if (goal.challenge == null) {
            return;
        }

        ChallengeEntity challenge = findOrThrowWithGoals(goal.challenge.id);
        if (challenge.status == ChallengeStatus.ARCHIVED) {
            return;
        }
        recomputeStoredStatus(challenge);
    }

    private void recomputeStoredStatus(ChallengeEntity challenge) {
        if (challenge.status == ChallengeStatus.ARCHIVED) {
            return;
        }

        boolean allCompleted = !challenge.goals.isEmpty() && challenge.goals.stream()
                .allMatch(goal -> goal.status == GoalStatus.COMPLETED);
        challenge.status = allCompleted ? ChallengeStatus.COMPLETED : ChallengeStatus.ACTIVE;
    }

    private ChallengeEntity findOrThrowWithGoals(UUID id) {
        return challengeRepository.findByIdWithGoals(id)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "Challenge not found: " + id));
    }

    private void validateCreateRequest(CreateChallengeRequest request, LocalDate challengeStartDate) {
        if (request.goals == null || request.goals.isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Challenge must contain at least one goal");
        }

        validateStartDateLowerLimit(challengeStartDate);
        validateDueDateNotPast(request.dueDate);
        validateDateRange(challengeStartDate, request.dueDate);

        for (CreateChallengeGoalRequest goal : request.goals) {
            if (goal.aimType == AimType.PERIODIC) {
                throw new ApiException(Response.Status.BAD_REQUEST,
                        "PERIODIC is reserved for a future phase");
            }
            if (goal.aimType == AimType.MULTI_STEP && goal.targetCount < 1) {
                throw new ApiException(Response.Status.BAD_REQUEST, "targetCount must be >= 1");
            }
        }
    }

    private void populateGoalFromChallengeRequest(GoalEntity goal,
                                                   CreateChallengeGoalRequest request,
                                                   LocalDate challengeStartDate,
                                                   LocalDate challengeDueDate) {
        goal.title = normalizeTitle(request.title);
        goal.description = normalizeDescription(request.description);
        goal.duration = request.duration;
        goal.aimType = request.aimType;
        goal.status = GoalStatus.ACTIVE;
        goal.targetCount = request.aimType == AimType.ONE_TIME ? 1 : request.targetCount;
        goal.currentCount = 0;
        goal.startDate = challengeStartDate;
        goal.dueDate = challengeDueDate;
    }

    private static void ensureNotArchived(ChallengeEntity challenge) {
        if (challenge.status == ChallengeStatus.ARCHIVED) {
            throw new ApiException(Response.Status.CONFLICT, "Archived challenges cannot be modified");
        }
    }

    private static void validateDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "dueDate must be equal to or after startDate");
        }
    }

    private static void validateStartDateLowerLimit(LocalDate startDate) {
        if (startDate != null && startDate.isBefore(LocalDate.now().minusMonths(1))) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "startDate cannot be older than one month before today");
        }
    }

    private static void validateDueDateNotPast(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "dueDate cannot be in the past");
        }
    }

    private static String normalizeTitle(String title) {
        String normalized = title == null ? null : title.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "title is required");
        }
        if (normalized.length() > 30) {
            throw new ApiException(Response.Status.BAD_REQUEST, "title must be <= 30 characters");
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 100) {
            throw new ApiException(Response.Status.BAD_REQUEST, "description must be <= 100 characters");
        }
        return normalized;
    }
}
