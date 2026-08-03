package com.example.demo.model;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    WAITING_FOR_LOGISTICS,
    RESOLVED,
    CLOSED;

    public boolean isOpen() {
        return this != RESOLVED && this != CLOSED;
    }

    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || target == this) {
            return target == this;
        }
        Set<TicketStatus> allowed = switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, WAITING_FOR_CUSTOMER,
                    WAITING_FOR_LOGISTICS, RESOLVED);
            case IN_PROGRESS -> EnumSet.of(OPEN, WAITING_FOR_CUSTOMER,
                    WAITING_FOR_LOGISTICS, RESOLVED);
            case WAITING_FOR_CUSTOMER, WAITING_FOR_LOGISTICS -> EnumSet.of(IN_PROGRESS, RESOLVED);
            case RESOLVED -> EnumSet.of(IN_PROGRESS, WAITING_FOR_LOGISTICS, CLOSED);
            case CLOSED -> EnumSet.noneOf(TicketStatus.class);
        };
        return allowed.contains(target);
    }
}
