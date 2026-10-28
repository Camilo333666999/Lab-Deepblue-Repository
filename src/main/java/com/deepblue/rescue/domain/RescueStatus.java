package com.deepblue.rescue.domain;

public enum RescueStatus {
    ADMITTED,
    UNDER_EVALUATION,
    IN_REHABILITATION,
    READY_FOR_RELEASE,
    RELEASED,
    CLOSED;

    public boolean canReceiveTreatment() {
        return this == UNDER_EVALUATION || this == IN_REHABILITATION;
    }
}