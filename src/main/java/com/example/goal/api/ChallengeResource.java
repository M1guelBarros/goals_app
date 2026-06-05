package com.example.goal.api;

import com.example.goal.api.dto.AddChallengeGoalsRequest;
import com.example.goal.api.dto.ChallengeResponse;
import com.example.goal.api.dto.CreateChallengeRequest;
import com.example.goal.api.dto.UpdateChallengeRequest;
import com.example.goal.application.ChallengeService;
import com.example.goal.domain.ChallengeStatus;
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

@Path("/api/challenges")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ChallengeResource {

    private final ChallengeService challengeService;

    public ChallengeResource(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @POST
    public ChallengeResponse create(@Valid CreateChallengeRequest request) {
        return challengeService.create(request);
    }

    @GET
    public List<ChallengeResponse> list(@QueryParam("status") ChallengeStatus status,
                                        @QueryParam("titleContains") String titleContains,
                                        @QueryParam("includeArchived") boolean includeArchived) {
        return challengeService.list(status, titleContains, includeArchived);
    }


    @GET
    @Path("/{id}")
    public ChallengeResponse get(@PathParam("id") UUID id) {
        return challengeService.get(id);
    }

    @PATCH
    @Path("/{id}")
    public ChallengeResponse update(@PathParam("id") UUID id, @Valid UpdateChallengeRequest request) {
        return challengeService.update(id, request);
    }

    @POST
    @Path("/{id}/goals")
    public ChallengeResponse addExistingGoals(@PathParam("id") UUID id, @Valid AddChallengeGoalsRequest request) {
        return challengeService.addExistingGoals(id, request);
    }

    @PATCH
    @Path("/{id}/archive")
    public ChallengeResponse archive(@PathParam("id") UUID id) {
        return challengeService.archive(id);
    }
}
