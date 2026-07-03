package com.myroutine.api.controller;

import com.myroutine.api.dto.CreateReminderRequest;
import com.myroutine.api.dto.ReminderResponse;
import com.myroutine.api.security.UserPrincipal;
import com.myroutine.domain.Reminder;
import com.myroutine.domain.ReminderFilters;
import com.myroutine.domain.ReminderStatus;
import com.myroutine.domain.SavedReminder;
import com.myroutine.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public List<SavedReminder> getReminders(@AuthenticationPrincipal UserPrincipal user,
                                            @RequestParam(required = false)ReminderStatus status,
                                            @RequestParam(required = false) String source,
                                            @RequestParam(required = false)Instant from,
                                            @RequestParam(required = false)Instant to
                                            ) {
        return reminderService.listReminders(user.getId(), new ReminderFilters(status, source, from, to));
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(
            @Valid @RequestBody CreateReminderRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SavedReminder saved = reminderService.createReminder(
                request.title(),
                request.dueAt(),
                user.getId(),
                request.recurrence());

        return ResponseEntity.status(HttpStatus.CREATED).body(ReminderResponse.from(saved));
    }
}
