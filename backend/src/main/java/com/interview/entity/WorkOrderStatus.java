package com.interview.entity;

public enum WorkOrderStatus {
    PENDING(1),
    ACCEPTED(2),
    REFUSED(3);

    private final int sortPriority;

    WorkOrderStatus(int sortPriority) {
        this.sortPriority = sortPriority;
    }

    public int getSortPriority() {
        return sortPriority;
    }
}
