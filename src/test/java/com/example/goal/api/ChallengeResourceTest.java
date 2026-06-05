package com.example.goal.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ChallengeResourceTest {

    @Test
    void createChallengeWithoutStartDateDefaultsToTodayAndChildrenInheritDates() {
        String dueDate = LocalDate.now().plusDays(10).toString();
        String today = LocalDate.now().toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Fitness sprint",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Run 5km",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            },
                            {
                              "title": "Workout sessions",
                              "duration": "SHORT_TERM",
                              "aimType": "MULTI_STEP",
                              "targetCount": 3
                            }
                          ]
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("status", equalTo("ACTIVE"))
                .body("startDate", equalTo(today))
                .body("dueDate", equalTo(dueDate))
                .body("goals.size()", equalTo(2))
                .body("goals[0].startDate", equalTo(today))
                .body("goals[1].startDate", equalTo(today))
                .body("goals[0].dueDate", equalTo(dueDate))
                .body("goals[1].dueDate", equalTo(dueDate));
    }

    @Test
    void createChallengeWithStartDateUsesProvidedValueAndChildrenInheritDates() {
        String startDate = LocalDate.now().minusDays(10).toString();
        String dueDate = LocalDate.now().plusDays(10).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Focused sprint",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Task one",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(startDate, dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .body("startDate", equalTo(startDate))
                .body("dueDate", equalTo(dueDate))
                .body("goals[0].startDate", equalTo(startDate))
                .body("goals[0].dueDate", equalTo(dueDate));
    }

    @Test
    void addExistingGoalWithDifferentDatesRequiresConfirmation() {
        String goalStartDate = LocalDate.now().plusDays(1).toString();
        String goalDueDate = LocalDate.now().plusDays(20).toString();
        String challengeStartDate = LocalDate.now().toString();
        String challengeDueDate = LocalDate.now().plusDays(10).toString();

        String goalId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Read 20 pages",
                          "duration": "MEDIUM_TERM",
                          "aimType": "ONE_TIME",
                          "startDate": "%s",
                          "dueDate": "%s"
                        }
                        """.formatted(goalStartDate, goalDueDate))
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Summer challenge",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Kickoff task",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(challengeStartDate, challengeDueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "goalIds": ["%s"]
                        }
                        """.formatted(goalId))
                .when()
                .post("/api/challenges/" + challengeId + "/goals")
                .then()
                .statusCode(409)
                .body("message", containsString("confirmDateChange=true"));
    }

    @Test
    void addExistingGoalWithConfirmationAlignsDatesAndPreservesProgressAndHistory() {
        String goalStartDate = LocalDate.now().plusDays(1).toString();
        String goalDueDate = LocalDate.now().plusDays(15).toString();
        String challengeStartDate = LocalDate.now().minusDays(2).toString();
        String challengeDueDate = LocalDate.now().plusDays(8).toString();

        String goalId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Prepare workshop",
                          "duration": "MEDIUM_TERM",
                          "aimType": "ONE_TIME",
                          "startDate": "%s",
                          "dueDate": "%s"
                        }
                        """.formatted(goalStartDate, goalDueDate))
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalId + "/increment")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("currentCount", equalTo(1));

        given()
                .when()
                .get("/api/goals/" + goalId + "/history")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Progress keeper",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Baseline task",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(challengeStartDate, challengeDueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "goalIds": ["%s"],
                          "confirmDateChange": true
                        }
                        """.formatted(goalId))
                .when()
                .post("/api/challenges/" + challengeId + "/goals")
                .then()
                .statusCode(200)
                .body("goals.size()", equalTo(2));

        given()
                .when()
                .get("/api/goals/" + goalId)
                .then()
                .statusCode(200)
                .body("startDate", equalTo(challengeStartDate))
                .body("dueDate", equalTo(challengeDueDate))
                .body("status", equalTo("COMPLETED"))
                .body("currentCount", equalTo(1))
                .body("challengeId", equalTo(challengeId));

        given()
                .when()
                .get("/api/goals/" + goalId + "/history")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void addExistingGoalsRejectsMissingOrInvalidBody() {
        String dueDate = LocalDate.now().plusDays(7).toString();

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Body validation challenge",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/challenges/" + challengeId + "/goals")
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/challenges/" + challengeId + "/goals")
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "goalIds": []
                        }
                        """)
                .when()
                .post("/api/challenges/" + challengeId + "/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void goalDatesCannotBeUpdatedDirectlyWhenGoalBelongsToChallenge() {
        String startDate = LocalDate.now().minusDays(3).toString();
        String dueDate = LocalDate.now().plusDays(12).toString();

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Study challenge",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Module 1",
                              "duration": "MEDIUM_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(startDate, dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String goalId = given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .extract()
                .path("goals[0].id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startDate": "%s"
                        }
                        """.formatted(LocalDate.now().toString()))
                .when()
                .patch("/api/goals/" + goalId)
                .then()
                .statusCode(409)
                .body("message", containsString("managed by its challenge"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "dueDate": "%s"
                        }
                        """.formatted(LocalDate.now().plusDays(20).toString()))
                .when()
                .patch("/api/goals/" + goalId)
                .then()
                .statusCode(409)
                .body("message", containsString("managed by its challenge"));
    }

    @Test
    void updatingChallengeDueDatePropagatesToChildGoalsAndKeepsStartDate() {
        String startDate = LocalDate.now().minusDays(5).toString();
        String initialDueDate = LocalDate.now().plusDays(9).toString();
        String newDueDate = LocalDate.now().plusDays(20).toString();

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Due date owner",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Due date child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(startDate, initialDueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "dueDate": "%s"
                        }
                        """.formatted(newDueDate))
                .when()
                .patch("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .body("startDate", equalTo(startDate))
                .body("dueDate", equalTo(newDueDate))
                .body("goals[0].startDate", equalTo(startDate))
                .body("goals[0].dueDate", equalTo(newDueDate));
    }

    @Test
    void challengeStatusRecomputesFromChildGoalProgressAndUndo() {
        String dueDate = LocalDate.now().plusDays(7).toString();

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Weekend challenge",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Task A",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            },
                            {
                              "title": "Task B",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String goalAId = given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .extract()
                .path("goals[0].id");

        String goalBId = given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .extract()
                .path("goals[1].id");

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalAId + "/increment")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalBId + "/increment")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalBId + "/undo-last")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    void archivingChallengeAlsoArchivesChildGoalsAndBlocksGoalMutations() {
        String dueDate = LocalDate.now().plusDays(15).toString();

        String challengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Archive together",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Child goal",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String goalId = given()
                .when()
                .get("/api/challenges/" + challengeId)
                .then()
                .statusCode(200)
                .extract()
                .path("goals[0].id");

        given()
                .when()
                .patch("/api/challenges/" + challengeId + "/archive")
                .then()
                .statusCode(200)
                .body("status", equalTo("ARCHIVED"))
                .body("goals[0].status", equalTo("ARCHIVED"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalId + "/increment")
                .then()
                .statusCode(409);

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + goalId + "/undo-last")
                .then()
                .statusCode(409);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "updated title"
                        }
                        """)
                .when()
                .patch("/api/goals/" + goalId)
                .then()
                .statusCode(409);
    }

    @Test
    void challengeListSupportsStatusAndTitleFilters() {
        String dueDateOne = LocalDate.now().plusDays(5).toString();
        String dueDateTwo = LocalDate.now().plusDays(6).toString();
        String dueDateThree = LocalDate.now().plusDays(7).toString();

        String activeChallengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Fitness Filter Alpha",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Active child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDateOne))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String completedChallengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Filter Completed",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Completed child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDateTwo))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String completedGoalId = given()
                .when()
                .get("/api/challenges/" + completedChallengeId)
                .then()
                .statusCode(200)
                .extract()
                .path("goals[0].id");

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + completedGoalId + "/increment")
                .then()
                .statusCode(200);

        String archivedChallengeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Filter Archived",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Archived child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDateThree))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .when()
                .patch("/api/challenges/" + archivedChallengeId + "/archive")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/challenges")
                .then()
                .statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(activeChallengeId), equalTo(1))
                .body("findAll { it.id == '%s' }.size()".formatted(completedChallengeId), equalTo(1))
                .body("findAll { it.id == '%s' }.size()".formatted(archivedChallengeId), equalTo(0));

        given()
                .when()
                .get("/api/challenges?status=ARCHIVED")
                .then()
                .statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(archivedChallengeId), equalTo(1));

        given()
                .when()
                .get("/api/challenges?status=COMPLETED")
                .then()
                .statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(completedChallengeId), equalTo(1));

        given()
                .when()
                .get("/api/challenges?titleContains=fitness")
                .then()
                .statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(activeChallengeId), equalTo(1));

        given()
                .when()
                .get("/api/challenges?includeArchived=true")
                .then()
                .statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(activeChallengeId), equalTo(1))
                .body("findAll { it.id == '%s' }.size()".formatted(completedChallengeId), equalTo(1))
                .body("findAll { it.id == '%s' }.size()".formatted(archivedChallengeId), equalTo(1));
    }

    @Test
    void createChallengeRejectsStartDateOlderThanOneMonthBeforeToday() {
        String oldStartDate = LocalDate.now().minusMonths(1).minusDays(1).toString();
        String dueDate = LocalDate.now().plusDays(2).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Too old start",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(oldStartDate, dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(400)
                .body("message", containsString("startDate cannot be older than one month before today"));
    }

    @Test
    void createChallengeRejectsPastDueDate() {
        String dueDate = LocalDate.now().minusDays(1).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Past due challenge",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(400)
                .body("message", containsString("dueDate cannot be in the past"));
    }

    @Test
    void createChallengeRejectsDueDateBeforeStartDate() {
        String startDate = LocalDate.now().plusDays(10).toString();
        String dueDate = LocalDate.now().plusDays(5).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Invalid range",
                          "startDate": "%s",
                          "dueDate": "%s",
                          "goals": [
                            {
                              "title": "Child",
                              "duration": "SHORT_TERM",
                              "aimType": "ONE_TIME"
                            }
                          ]
                        }
                        """.formatted(startDate, dueDate))
                .when()
                .post("/api/challenges")
                .then()
                .statusCode(400)
                .body("message", containsString("dueDate must be equal to or after startDate"));
    }
}
