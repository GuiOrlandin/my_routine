package com.myroutine.repository;

import com.myroutine.domain.Reminder;
import com.myroutine.domain.SavedReminder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * POO: INTERFACE — contrato de persistência sem expor detalhes de implementação (Supabase, SQL, etc.).
 *
 * <p>SOLID — DIP (Dependency Inversion): {@code ReminderService} depende desta abstração, não de uma
 * classe concreta. ISP (Interface Segregation): apenas as operações necessárias para lembretes.
 *
 * <p>Polimorfismo: qualquer implementação ({@code SupabaseReminderRepository}, in-memory para testes)
 * pode ser injetada sem alterar quem consome o contrato.
 */
public interface ReminderRepository {

    SavedReminder save(Reminder reminder);

    Optional<Reminder> findById(UUID id);

    List<Reminder> findByUserId(String userId);

    Reminder update(UUID id, Reminder reminder);

    void delete(UUID id);
}
