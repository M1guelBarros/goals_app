package com.example.goal.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GoalResourceTest {

    @Test
    void createMultiStepGoalAndProgressIt() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Read architecture book",
                          "duration": "MEDIUM_TERM",
                          "aimType": "MULTI_STEP",
                          "targetCount": 3
                        }
                        """)
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("status", equalTo("ACTIVE"))
                .body("currentCount", equalTo(0))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(1))
                .body("status", equalTo("ACTIVE"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(2))
                .body("status", equalTo("ACTIVE"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(3))
                .body("status", equalTo("COMPLETED"));

        given()
                .when()
                .get("/api/goals?status=COMPLETED")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/goals/" + id + "/history")
                .then()
                .statusCode(200)
                .body("", hasSize(3))
                .body("[0].progressType", equalTo("INCREMENT"))
                .body("[0].resultingCount", equalTo(1))
                .body("[1].progressType", equalTo("INCREMENT"))
                .body("[1].resultingCount", equalTo(2))
                .body("[2].progressType", equalTo("COMPLETE"))
                .body("[2].resultingCount", equalTo(3));
    }

    @Test
    void multiStepCompletionEventIsRecordedOnceAtThreshold() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Practice piano",
                          "duration": "SHORT_TERM",
                          "aimType": "MULTI_STEP",
                          "targetCount": 2
                        }
                        """)
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
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(1))
                .body("status", equalTo("ACTIVE"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(2))
                .body("status", equalTo("COMPLETED"));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("currentCount", equalTo(3))
                .body("status", equalTo("COMPLETED"));

        given()
                .when()
                .get("/api/goals/" + id + "/history")
                .then()
                .statusCode(200)
                .body("", hasSize(3))
                .body("findAll { it.progressType == 'COMPLETE' }.size()", equalTo(1))
                .body("[0].progressType", equalTo("INCREMENT"))
                .body("[1].progressType", equalTo("COMPLETE"))
                .body("[2].progressType", equalTo("INCREMENT"));
    }

    @Test
    void undoOneTimeGoalAlwaysReactivatesGoal() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Schedule annual checkup",
                          "duration": "LONG_TERM",
                          "aimType": "ONE_TIME"
                        }
                        """)
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("currentCount", equalTo(0))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/increment")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("currentCount", equalTo(1));

        given()
                .when()
                .get("/api/goals/" + id + "/history")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].progressType", equalTo("COMPLETE"))
                .body("[0].resultingCount", equalTo(1));

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/goals/" + id + "/undo-last")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("currentCount", equalTo(0));
    }

    @Test
    void createGoalRejectsStartDateOlderThanOneMonthBeforeToday() {
        String oldStartDate = LocalDate.now().minusMonths(1).minusDays(1).toString();
        String dueDate = LocalDate.now().plusDays(10).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Old start date goal",
                          "duration": "SHORT_TERM",
                          "aimType": "ONE_TIME",
                          "startDate": "%s",
                          "dueDate": "%s"
                        }
                        """.formatted(oldStartDate, dueDate))
                .when()
                .post("/api/goals")
                .then()
                .statusCode(400)
                .body("message", containsString("startDate cannot be older than one month before today"));
    }

    @Test
    void createGoalRejectsPastDueDate() {
        String dueDate = LocalDate.now().minusDays(1).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Past due date goal",
                          "duration": "SHORT_TERM",
                          "aimType": "ONE_TIME",
                          "dueDate": "%s"
                        }
                        """.formatted(dueDate))
                .when()
                .post("/api/goals")
                .then()
                .statusCode(400)
                .body("message", containsString("dueDate cannot be in the past"));
    }

    @Test
    void updateGoalRejectsStartDateOlderThanOneMonthBeforeToday() {
        String goalId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Update start date goal",
                          "duration": "SHORT_TERM",
                          "aimType": "ONE_TIME"
                        }
                        """)
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String oldStartDate = LocalDate.now().minusMonths(1).minusDays(1).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startDate": "%s"
                        }
                        """.formatted(oldStartDate))
                .when()
                .patch("/api/goals/" + goalId)
                .then()
                .statusCode(400)
                .body("message", containsString("startDate cannot be older than one month before today"));
    }

    @Test
    void updateGoalRejectsPastDueDate() {
        String goalId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Update due date goal",
                          "duration": "SHORT_TERM",
                          "aimType": "ONE_TIME"
                        }
                        """)
                .when()
                .post("/api/goals")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        String pastDueDate = LocalDate.now().minusDays(1).toString();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "dueDate": "%s"
                        }
                        """.formatted(pastDueDate))
                .when()
                .patch("/api/goals/" + goalId)
                .then()
                .statusCode(400)
                .body("message", containsString("dueDate cannot be in the past"));
    }
}
