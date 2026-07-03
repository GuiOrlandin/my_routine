package com.myroutine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ReminderTest {

    @Test
    void createsValidReminder() {
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder reminder = new Reminder("Reunião", dueAt, "user-123");

        assertEquals("Reunião", reminder.getTitle());
        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        assertEquals(Recurrence.NONE, reminder.getRecurrence());
    }

    @Test
    void rejectsEmptyTitle() {
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        assertThrows(IllegalArgumentException.class, () -> new Reminder("  ", dueAt, "user-123"));
    }

    @Test
    void rejectsPastDueDate() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        assertThrows(IllegalArgumentException.class, () -> new Reminder("Teste", past, "user-123"));
    }

    @Test
    void markDoneUpdatesStatus() {
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder reminder = new Reminder("Teste", dueAt, "user-123");

        reminder.markDone();

        assertEquals(ReminderStatus.DONE, reminder.getStatus());
    }

    @Test
    void snoozeUpdatesDueAtAndStatus() {
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant newDueAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Reminder reminder = new Reminder("Teste", dueAt, "user-123");

        reminder.snooze(newDueAt);

        assertEquals(newDueAt, reminder.getDueAt());
        assertEquals(ReminderStatus.SNOOZED, reminder.getStatus());
    }
}
