package com.bods.preflight.model;

public enum Severity {
    CRITICAL(5),
    ERROR(4),
    WARNING(3),
    INFO(2),
    PASS(1);

    private final int priority;

    Severity(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
