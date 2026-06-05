package com.example.goal.domain;

/**
 * Determines how a goal reaches completion.
 * PERIODIC stays as a reserved future type in this MVP.
 */
public enum AimType {
    ONE_TIME,
    MULTI_STEP,
    PERIODIC
}
