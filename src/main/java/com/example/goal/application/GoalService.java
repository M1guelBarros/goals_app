package com.example.goal.application;

import com.example.goal.api.dto.CreateGoalRequest;
import com.example.goal.api.dto.HistoryResponse;
import com.example.goal.api.dto.GoalResponse;
import com.example.goal.api.dto.IncrementRequest;
import com.example.goal.api.dto.UpdateGoalRequest;
import com.example.goal.domain.AimType;
import com.example.goal.domain.ChallengeStatus;
import com.example.goal.domain.Duration;
import com.example.goal.domain.GoalStatus;
import com.example.goal.domain.ProgressType;
import com.example.goal.persistence.GoalEntity;
import com.example.goal.persistence.HistoryEntity;
import com.example.goal.persistence.HistoryRepository;
import com.example.goal.persistence.GoalRepository;
import com.example.goal.shared.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application layer for goal use cases.
 *
 * @Transactional on public write methods ensures that each business action is atomic,
 * including both aggregate updates and progress-history persistence.
 */
@ApplicationScoped
public class GoalService {

    private final GoalRepository goalRepository;
    private final HistoryRepository historyRepository;
    private final ChallengeService challengeService;

    public GoalService(GoalRepository goalRepository,
                       HistoryRepository historyRepository,
                       ChallengeService challengeService) {
        this.goalRepository = goalRepository;
        this.historyRepository = historyRepository;
        this.challengeService = challengeService;
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest request) {
        validateCreation(request);

        GoalEntity entity = new GoalEntity();
        entity.title = normalizeTitle(request.title);
        entity.description = normalizeDescription(request.description);
        entity.duration = request.duration;
        entity.aimType = request.aimType;
        entity.status = GoalStatus.ACTIVE;
        entity.targetCount = request.aimType == AimType.ONE_TIME ? 1 : request.targetCount;
        entity.currentCount = 0;
        entity.startDate = request.startDate;
        entity.dueDate = request.dueDate;

        goalRepository.persist(entity);
        return GoalMapper.toResponse(entity);
    }

    public GoalResponse get(UUID id) {
        return GoalMapper.toResponse(findOrThrow(id));
    }

    public List<GoalResponse> list(Duration duration,
                                   GoalStatus status,
                                   AimType aimType,
                                   String titleContains,
                                   boolean includeArchived) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();

        if (!includeArchived && status == null) {
            query.append(" and status <> ?").append(params.size() + 1);
            params.add(GoalStatus.ARCHIVED);
        }
        if (duration != null) {
            query.append(" and duration = ?").append(params.size() + 1);
            params.add(duration);
        }
        if (status != null) {
            query.append(" and status = ?").append(params.size() + 1);
            params.add(status);
        }
        if (aimType != null) {
            query.append(" and aimType = ?").append(params.size() + 1);
            params.add(aimType);
        }
        if (titleContains != null && !titleContains.isBlank()) {
            query.append(" and lower(title) like ?").append(params.size() + 1);
            params.add("%" + titleContains.toLowerCase() + "%");
        }

        return goalRepository.find(query + " order by createdAt desc", params.toArray())
                .list()
                .stream()
                .map(GoalMapper::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse updateEditableFields(UUID id, UpdateGoalRequest request) {
        GoalEntity entity = findOrThrow(id);
        ensureNotArchived(entity);
        ensureParentChallengeNotArchived(entity);

        if (request.title != null) {
            entity.title = normalizeTitle(request.title);
        }
        if (request.description != null) {
            entity.description = normalizeDescription(request.description);
        }
        if (request.duration != null) {
            entity.duration = request.duration;
        }
        if ((request.startDate != null || request.dueDate != null) && entity.challenge != null) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Goal dates are managed by its challenge. Update the challenge dueDate instead");
        }
        if (request.startDate != null || request.dueDate != null) {
            LocalDate newStartDate = request.startDate != null ? request.startDate : entity.startDate;
            LocalDate newDueDate = request.dueDate != null ? request.dueDate : entity.dueDate;
            if (request.startDate != null) {
                validateStartDateLowerLimit(request.startDate);
            }
            if (request.dueDate != null) {
                validateDueDateNotPast(request.dueDate);
            }
            validateDateRange(newStartDate, newDueDate);
            entity.startDate = newStartDate;
            entity.dueDate = newDueDate;
        }

        return GoalMapper.toResponse(entity);
    }

    @Transactional
    public GoalResponse incrementGoal(UUID id, IncrementRequest request) {
        GoalEntity entity = findOrThrow(id);
        ensureNotArchived(entity);
        ensureParentChallengeNotArchived(entity);

        if (entity.aimType == AimType.ONE_TIME) {
            completeOneTime(entity, request);
            challengeService.recomputeStatusForGoal(entity.id);
            return GoalMapper.toResponse(entity);
        }

        if (entity.aimType == AimType.MULTI_STEP) {
            int amount = 1;
            entity.currentCount = entity.currentCount + amount;
            int currentCount = entity.currentCount;

            ProgressType progressType = currentCount == entity.targetCount
                    ? ProgressType.COMPLETE
                    : ProgressType.INCREMENT;
            addHistoryEntry(entity, progressType, amount, currentCount, request == null ? null : request.eventDate);

            entity.status = currentCount >= entity.targetCount
                    ? GoalStatus.COMPLETED
                    : GoalStatus.ACTIVE;

            challengeService.recomputeStatusForGoal(entity.id);
            return GoalMapper.toResponse(entity);
        }

        throw new ApiException(Response.Status.CONFLICT,
                "Operation not allowed for aimType " + entity.aimType);
    }

    @Transactional
    public GoalResponse undoLastProgress(UUID id) {
        GoalEntity entity = findOrThrow(id);
        ensureNotArchived(entity);
        ensureParentChallengeNotArchived(entity);

        HistoryEntity last = historyRepository.findLatestByGoalId(id)
                .orElseThrow(() -> new ApiException(Response.Status.CONFLICT, "No progress history to undo"));

        historyRepository.delete(last);
        entity.currentCount = Math.max(0, entity.currentCount - last.amount);

        if (entity.aimType == AimType.ONE_TIME) {
            entity.status = GoalStatus.ACTIVE;
        } else if (entity.aimType == AimType.MULTI_STEP) {
            entity.status = entity.currentCount >= entity.targetCount
                    ? GoalStatus.COMPLETED
                    : GoalStatus.ACTIVE;
        }

        challengeService.recomputeStatusForGoal(entity.id);
        return GoalMapper.toResponse(entity);
    }

    public List<HistoryResponse> getHistory(UUID id) {
        findOrThrow(id);
        return historyRepository.findHistoryByGoalId(id)
                .stream()
                .map(GoalMapper::toHistoryResponse)
                .toList();
    }

    private void validateCreation(CreateGoalRequest request) {
        if (request.aimType == AimType.PERIODIC) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "PERIODIC is reserved for a future phase");
        }

        validateStartDateLowerLimit(request.startDate);
        validateDueDateNotPast(request.dueDate);
        validateDateRange(request.startDate, request.dueDate);

        if (request.aimType == AimType.MULTI_STEP && request.targetCount < 1) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "targetCount must be >= 1");
        }
    }

    private void addHistoryEntry(GoalEntity goal,
                                 ProgressType progressType,
                                 int amount,
                                 int resultingCount,
                                 LocalDate eventDate) {
        HistoryEntity record = new HistoryEntity();
        record.goal = goal;
        record.progressType = progressType;
        record.amount = amount;
        record.resultingCount = resultingCount;
        record.eventDate = eventDate == null ? LocalDate.now() : eventDate;
        historyRepository.persist(record);
    }

    private void completeOneTime(GoalEntity entity, IncrementRequest request) {
        // ONE_TIME completion is intentionally idempotent.
        if (entity.currentCount == 0) {
            entity.currentCount = 1;
            entity.status = GoalStatus.COMPLETED;
            addHistoryEntry(entity, ProgressType.COMPLETE, 1, entity.currentCount,
                    request == null ? null : request.eventDate);
        }
    }

    private GoalEntity findOrThrow(UUID id) {
        return goalRepository.findByIdOptional(id)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "Goal not found: " + id));
    }

    private static void ensureNotArchived(GoalEntity entity) {
        if (entity.status == GoalStatus.ARCHIVED) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Archived goals cannot be modified or progressed");
        }
    }

    private static void ensureParentChallengeNotArchived(GoalEntity entity) {
        if (entity.challenge != null && entity.challenge.status == ChallengeStatus.ARCHIVED) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Goals inside archived challenges cannot be modified or progressed");
        }
    }

/*    private static void ensureAimType(GoalEntity entity, AimType expected) {
        if (entity.aimType != expected) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Operation not allowed for aimType " + entity.aimType);
        }
    }
*/
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
