package com.myroutine.service;

import com.myroutine.api.dto.ReminderResponse;
import com.myroutine.domain.Recurrence;
import com.myroutine.domain.Reminder;
import com.myroutine.domain.ReminderFilters;
import com.myroutine.domain.SavedReminder;
import com.myroutine.repository.ReminderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * POO: COMPOSIÇÃO — orquestra {@link Reminder} (domínio) e {@link ReminderRepository} (persistência).
 *
 * <p>SOLID — SRP: única responsabilidade é coordenar casos de uso de lembretes, sem HTTP nem SQL.
 * DIP: depende da abstração {@code ReminderRepository}, injetada pelo Spring via construtor.
 */
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    /**
     * Cria lembrete validando regras no construtor de {@link Reminder} (título, data futura, userId).
     */
    public SavedReminder createReminder(String title, Instant dueAt, String userId, Recurrence recurrence) {
        Reminder reminder = new Reminder(title, dueAt, userId, recurrence);
        return reminderRepository.save(reminder);
    }

    public List<SavedReminder> listReminders(String userId, ReminderFilters filters) {
        return reminderRepository.findByUserId(userId, filters);
    }

    public Reminder markDone(UUID id, String userId) {
        Reminder reminder = requireOwnedReminder(id, userId);
        reminder.markDone();
        return reminderRepository.update(id, reminder);
    }

    public void deleteReminder(UUID id, String userId) {
        requireOwnedReminder(id, userId);
        reminderRepository.delete(id);
    }

    private Reminder requireOwnedReminder(UUID id, String userId) {
        return reminderRepository.findById(id)
                .filter(reminder -> reminder.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Lembrete não encontrado"));
    }
}
