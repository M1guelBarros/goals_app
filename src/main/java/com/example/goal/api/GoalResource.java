package com.example.goal.api;

import com.example.goal.api.dto.CreateGoalRequest;
import com.example.goal.api.dto.HistoryResponse;
import com.example.goal.api.dto.GoalResponse;
import com.example.goal.api.dto.IncrementRequest;
import com.example.goal.api.dto.UpdateGoalRequest;
import com.example.goal.application.GoalService;
import com.example.goal.domain.AimType;
import com.example.goal.domain.Duration;
import com.example.goal.domain.GoalStatus;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/**
 * REST API for goals.
 *
 * Quarkus discovers this resource via @Path and handles JSON serialization
 * through the REST + Jackson extension.
 */
@Path("/api/goals")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GoalResource {

    private final GoalService goalService;

    public GoalResource(GoalService goalService) {
        this.goalService = goalService;
    }

    @POST
    public GoalResponse create(@Valid CreateGoalRequest request) {
        return goalService.create(request);
    }

    @GET
    public List<GoalResponse> list(@QueryParam("duration") Duration duration,
                                   @QueryParam("status") GoalStatus status,
                                   @QueryParam("aimType") AimType aimType,
                                   @QueryParam("titleContains") String titleContains,
                                   @QueryParam("includeArchived") boolean includeArchived) {
        return goalService.list(duration, status, aimType, titleContains, includeArchived);
    }

    @GET
    @Path("/{id}")
    public GoalResponse get(@PathParam("id") UUID id) {
        return goalService.get(id);
    }

    @PATCH
    @Path("/{id}")
    public GoalResponse update(@PathParam("id") UUID id, @Valid UpdateGoalRequest request) {
        return goalService.updateEditableFields(id, request);
    }

    @POST
    @Path("/{id}/increment")
    public GoalResponse increment(@PathParam("id") UUID id, IncrementRequest request) {
        return goalService.incrementGoal(id, request);
    }

    @POST
    @Path("/{id}/undo-last")
    public GoalResponse undoLastProgress(@PathParam("id") UUID id) {
        return goalService.undoLastProgress(id);
    }

    @GET
    @Path("/{id}/history")
    public List<HistoryResponse> history(@PathParam("id") UUID id) {
        return goalService.getHistory(id);
    }
}
