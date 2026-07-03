package com.myroutine.api.controller;

import com.myroutine.api.dto.CreateReminderRequest;
import com.myroutine.api.dto.ReminderResponse;
import com.myroutine.api.security.UserPrincipal;
import com.myroutine.domain.SavedReminder;
import com.myroutine.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
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
