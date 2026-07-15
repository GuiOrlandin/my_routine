package com.myroutine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myroutine.api.dto.UpdateReminderRequest;
import com.myroutine.domain.Recurrence;
import com.myroutine.domain.Reminder;
import com.myroutine.domain.ReminderFilters;
import com.myroutine.domain.ReminderStatus;
import com.myroutine.domain.SavedReminder;
import com.myroutine.repository.ReminderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Seam: {@link ReminderService} com {@link ReminderRepository} mockado (borda de persistência).
 *
 * <p>Critérios T19: criar via serviço, rejeitar data passada, marcar concluído com mock repository.
 */
class ReminderServiceTest {

    private ReminderRepository reminderRepository;
    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderRepository = mock(ReminderRepository.class);
        reminderService = new ReminderService(reminderRepository);
    }

    @Test
    void createReminderPersistsValidReminder() {
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder expected = new Reminder("Comprar pão", dueAt, "user-1", Recurrence.DAILY);
        SavedReminder saved = new SavedReminder(
                expected, UUID.randomUUID(), "manual", Instant.now(), Instant.now());
        when(reminderRepository.save(any(Reminder.class))).thenReturn(saved);

        SavedReminder result =
                reminderService.createReminder("Comprar pão", dueAt, "user-1", Recurrence.DAILY);

        assertEquals(saved, result);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals("Comprar pão", captor.getValue().getTitle());
        assertEquals(dueAt, captor.getValue().getDueAt());
        assertEquals(ReminderStatus.PENDING, captor.getValue().getStatus());
        assertEquals(Recurrence.DAILY, captor.getValue().getRecurrence());
    }

    @Test
    void createReminderRejectsPastDueDate() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        assertThrows(
                IllegalArgumentException.class,
                () -> reminderService.createReminder("Atrasado", past, "user-1", Recurrence.NONE));

        verify(reminderRepository, never()).save(any());
    }

    @Test
    void updateReminderMarksDone() {
        UUID id = UUID.randomUUID();
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder owned = Reminder.fromPersistence(
                "Reunião", dueAt, "user-1", Recurrence.NONE, ReminderStatus.PENDING);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(owned));

        Reminder done = Reminder.fromPersistence(
                "Reunião", dueAt, "user-1", Recurrence.NONE, ReminderStatus.DONE);
        SavedReminder updated =
                new SavedReminder(done, id, "manual", Instant.now(), Instant.now());
        when(reminderRepository.update(eq(id), any(Reminder.class))).thenReturn(updated);

        SavedReminder result = reminderService.updateReminder(
                id, "user-1", new UpdateReminderRequest(null, null, ReminderStatus.DONE, null));

        assertEquals(ReminderStatus.DONE, result.reminder().getStatus());
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).update(eq(id), captor.capture());
        assertEquals(ReminderStatus.DONE, captor.getValue().getStatus());
    }

    @Test
    void listRemindersReturnsRepositoryResults() {
        ReminderFilters filters = new ReminderFilters(null, null, null, null);
        SavedReminder item = new SavedReminder(
                new Reminder("Item", Instant.now().plus(1, ChronoUnit.DAYS), "user-1"),
                UUID.randomUUID(),
                "manual",
                Instant.now(),
                Instant.now());
        when(reminderRepository.findByUserId("user-1", filters)).thenReturn(List.of(item));

        List<SavedReminder> result = reminderService.listReminders("user-1", filters);

        assertEquals(1, result.size());
        assertEquals(item, result.getFirst());
    }

    @Test
    void deleteReminderRemovesOwnedReminder() {
        UUID id = UUID.randomUUID();
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder owned = Reminder.fromPersistence(
                "Apagar", dueAt, "user-1", Recurrence.NONE, ReminderStatus.PENDING);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(owned));

        reminderService.deleteReminder(id, "user-1");

        verify(reminderRepository).delete(id);
    }

    @Test
    void updateReminderOfOtherUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Reminder other = Reminder.fromPersistence(
                "Alheio", dueAt, "other-user", Recurrence.NONE, ReminderStatus.PENDING);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(other));

        assertThrows(
                NoSuchElementException.class,
                () -> reminderService.updateReminder(
                        id,
                        "user-1",
                        new UpdateReminderRequest(null, null, ReminderStatus.DONE, null)));

        verify(reminderRepository, never()).update(any(), any());
    }
}
